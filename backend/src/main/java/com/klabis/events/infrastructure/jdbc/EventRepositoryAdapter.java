package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.jdbc.FulltextSearch;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


/**
 * Adapter that bridges between EventRepository domain interface and EventJdbcRepository.
 * <p>
 * It handles conversion between Event entities and EventMemento persistence objects.
 * <p>
 * Event publishing is handled automatically by Spring Modulith via the outbox pattern.
 * The EventMemento delegates @DomainEvents and @AfterDomainEventPublication to the Event entity.
 */
@SecondaryAdapter
@Repository
class EventRepositoryAdapter implements EventRepository {

    private final EventJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;

    public EventRepositoryAdapter(EventJdbcRepository jdbcRepository,
                                   JdbcAggregateTemplate jdbcAggregateTemplate,
                                   NamedParameterJdbcTemplate namedJdbc) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public Event save(Event event) {
        EventMemento saved = jdbcRepository.save(EventMemento.from(event));
        return saved.toEvent();
    }

    @Override
    public Optional<Event> findById(EventId eventId) {
        return jdbcRepository.findById(eventId.value())
                .map(EventMemento::toEvent);
    }

    @Override
    public Page<Event> findAll(EventFilter filter, Pageable pageable) {
        if (filter.statuses().isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<Criteria> conditions = new ArrayList<>();
        List<UUID> matchingIds = resolveMatchingIds(filter);
        if (matchingIds != null) {
            if (matchingIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            conditions.add(Criteria.where("id").in(matchingIds));
        }

        if (filter.statuses().size() < EventStatus.values().length) {
            conditions.add(Criteria.where("status").in(filter.statuses().stream().map(EventStatus::name).toList()));
        }
        if (filter.organizer() != null) {
            conditions.add(Criteria.where("organizer").is(filter.organizer()));
        }
        if (filter.dateFrom() != null) {
            conditions.add(Criteria.where("eventDate").greaterThanOrEquals(filter.dateFrom()));
        }
        if (filter.dateTo() != null) {
            conditions.add(Criteria.where("eventDate").lessThanOrEquals(filter.dateTo()));
        }
        if (!filter.eventTypeIds().isEmpty()) {
            conditions.add(Criteria.where("eventTypeId").in(filter.eventTypeIds().stream().map(EventTypeId::value).toList()));
        }
        if (filter.notRegisteredBy() != null) {
            List<UUID> registeredIds = findIdsByRegisteredMember(filter.notRegisteredBy());
            if (!registeredIds.isEmpty()) {
                conditions.add(Criteria.where("id").notIn(registeredIds));
            }
        }
        if (filter.cancelledVisibleTo() != null && !filter.excludesStatus(EventStatus.CANCELLED)) {
            Criteria notCancelled = Criteria.where("status").not(EventStatus.CANCELLED.name());
            List<UUID> registeredIds = findIdsByRegisteredMember(filter.cancelledVisibleTo());
            conditions.add(registeredIds.isEmpty()
                    ? notCancelled
                    : notCancelled.or(Criteria.where("id").in(registeredIds)));
        }

        Query query = conditions.stream().reduce(Criteria::and).map(Query::query).orElseGet(Query::empty);

        List<Event> results = jdbcAggregateTemplate.findAll(query.with(pageable), EventMemento.class)
                .stream()
                .map(EventMemento::toEvent)
                .toList();
        long total = pageable.isUnpaged() ? results.size() : jdbcAggregateTemplate.count(query, EventMemento.class);
        return new PageImpl<>(results, pageable, total);
    }

    /**
     * Intersection of the id sets of all active id-based constraints, or null when none is active.
     */
    private List<UUID> resolveMatchingIds(EventFilter filter) {
        List<List<UUID>> activeSets = new ArrayList<>();
        if (filter.fulltextQuery() != null) {
            activeSets.add(FulltextSearch.findIds(namedJdbc, "events.events", filter.fulltextQuery(),
                    "name", "coalesce(location, '')"));
        }
        if (filter.registeredBy() != null) {
            activeSets.add(findIdsByRegisteredMember(filter.registeredBy()));
        }
        if (filter.deadlineWithin() != null) {
            LocalDate today = LocalDate.now();
            activeSets.add(findIdsByDeadlineWithin(today, today.plus(filter.deadlineWithin())));
        }
        if (filter.coordinator() != null) {
            activeSets.add(findIdsCoordinatedBy(filter.coordinator()).stream().map(EventId::value).toList());
        }

        return activeSets.stream()
                .reduce((a, b) -> {
                    Set<UUID> bSet = Set.copyOf(b);
                    return a.stream().filter(bSet::contains).toList();
                })
                .orElse(null);
    }

    /**
     * Ids of events the member has a registration for.
     * Event status is intentionally not filtered — cancelled and finished events
     * with a live registration are included (spec requirement).
     */
    private List<UUID> findIdsByRegisteredMember(MemberId memberId) {
        String sql = """
                SELECT id FROM events.events e
                WHERE EXISTS (
                    SELECT 1 FROM events.event_registrations er
                    WHERE er.event_id = e.id AND er.member_id = :memberId
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("memberId", memberId.uuid());
        return namedJdbc.query(sql, params, (rs, rowNum) -> rs.getObject(1, UUID.class));
    }

    /**
     * Returns IDs of events whose nearest future registration deadline falls within
     * {@code [today, deadlineUntil]} (both inclusive).
     * <p>
     * Nearest future deadline logic (mirrors {@link com.klabis.events.domain.RegistrationDeadlines#nextRelevant}):
     * - If deadline1 is strictly after today → use deadline1
     * - Else if deadline2 is strictly after today → use deadline2
     * - Else if deadline3 is strictly after today → use deadline3
     * - Else fall back to the last non-null deadline (all are past or today)
     * <p>
     * Events with no deadlines configured are excluded.
     */
    private List<UUID> findIdsByDeadlineWithin(LocalDate today, LocalDate deadlineUntil) {
        String sql = """
                SELECT id FROM events.events e
                WHERE e.registration_deadline IS NOT NULL
                  AND CASE
                        WHEN e.registration_deadline > :today THEN e.registration_deadline
                        WHEN e.registration_deadline_2 IS NOT NULL AND e.registration_deadline_2 > :today THEN e.registration_deadline_2
                        WHEN e.registration_deadline_3 IS NOT NULL AND e.registration_deadline_3 > :today THEN e.registration_deadline_3
                        ELSE COALESCE(e.registration_deadline_3, e.registration_deadline_2, e.registration_deadline)
                      END BETWEEN :today AND :deadlineUntil
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("today", today)
                .addValue("deadlineUntil", deadlineUntil);
        return namedJdbc.query(sql, params, (rs, rowNum) -> rs.getObject(1, UUID.class));
    }

    @Override
    public List<EventId> findActiveIds(Collection<EventId> eventIds) {
        if (eventIds.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT id FROM events.events WHERE status = 'ACTIVE' AND id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource("ids", eventIds.stream().map(EventId::value).toList());
        return namedJdbc.query(sql, params, (rs, rowNum) -> new EventId(rs.getObject(1, UUID.class)));
    }

    @Override
    public List<EventId> findIdsCoordinatedBy(MemberId memberId) {
        String sql = "SELECT event_id FROM events.event_coordinators WHERE member_id = :memberId";
        MapSqlParameterSource params = new MapSqlParameterSource("memberId", memberId.uuid());
        return namedJdbc.query(sql, params, (rs, rowNum) -> new EventId(rs.getObject(1, UUID.class)));
    }
}
