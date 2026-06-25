package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.pagination.TranslatedPageable;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * Adapter that bridges between EventRepository domain interface and EventJdbcRepository.
 * <p>
 * This adapter implements:
 * <ul>
 *   <li>{@link EventRepository EventRepository} - domain repository interface</li>
 * </ul>
 * <p>
 * EventRepository extends {@link com.klabis.events.domain.Events Events} public API, so this adapter indirectly implements both.
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

    /**
     * Maps domain property names to database column names for sorting.
     */
    private static final Map<String, String> DOMAIN_TO_DB_COLUMN = Map.of(
            "eventDate", "event_date",
            "id", "id",
            "name", "name",
            "location", "location",
            "organizer", "organizer",
            "status", "status",
            "registrationDeadline", "registration_deadline"
    );

    public EventRepositoryAdapter(EventJdbcRepository jdbcRepository,
                                   JdbcAggregateTemplate jdbcAggregateTemplate,
                                   NamedParameterJdbcTemplate namedJdbc) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
        this.namedJdbc = namedJdbc;
    }

    /**
     * Translates Pageable with domain property names to Pageable with database column names.
     * Unpaged requests are returned as-is — there are no sort properties to translate.
     */
    private Pageable translateDomainToDbColumn(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        return TranslatedPageable.translate(pageable, DOMAIN_TO_DB_COLUMN);
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
        List<UUID> preFilteredIds = resolvePreFilteredIds(filter);

        if (preFilteredIds != null && preFilteredIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        if (preFilteredIds != null) {
            return findAllWithMatchingIds(filter, pageable, preFilteredIds);
        }

        return executeQuery(buildCriteriaQuery(filter), pageable);
    }

    /**
     * Returns the intersection of IDs that satisfy the fulltext, registeredBy,
     * deadlineWithin, and/or notRegisteredBy constraints,
     * or null if none of those constraints is active (meaning no pre-filtering is needed).
     */
    private List<UUID> resolvePreFilteredIds(EventFilter filter) {
        List<UUID> fulltextIds = filter.fulltextQuery() != null
                ? findIdsByFulltext(filter.fulltextQuery())
                : null;

        List<UUID> registeredByIds = filter.registeredBy() != null
                ? findIdsByRegisteredMember(filter.registeredBy())
                : null;

        LocalDate today = LocalDate.now();

        List<UUID> deadlineWithinIds = filter.deadlineWithin() != null
                ? findIdsByDeadlineWithin(today, today.plus(filter.deadlineWithin()))
                : null;

        List<UUID> notRegisteredByIds = filter.notRegisteredBy() != null
                ? findIdsByNotRegisteredMember(filter.notRegisteredBy())
                : null;

        List<UUID> coordinatorIds = filter.coordinator() != null
                ? findIdsByCoordinator(filter.coordinator())
                : null;

        List<List<UUID>> activeSets = new ArrayList<>();
        if (fulltextIds != null) activeSets.add(fulltextIds);
        if (registeredByIds != null) activeSets.add(registeredByIds);
        if (deadlineWithinIds != null) activeSets.add(deadlineWithinIds);
        if (notRegisteredByIds != null) activeSets.add(notRegisteredByIds);
        if (coordinatorIds != null) activeSets.add(coordinatorIds);

        if (activeSets.isEmpty()) {
            return null;
        }

        return activeSets.stream()
                .reduce((a, b) -> {
                    Set<UUID> bSet = Set.copyOf(b);
                    return a.stream().filter(bSet::contains).toList();
                })
                .orElse(List.of());
    }

    private Page<Event> findAllWithMatchingIds(EventFilter filter, Pageable pageable, List<UUID> matchingIds) {
        List<Criteria> conditions = new ArrayList<>();
        conditions.add(Criteria.where("id").in(matchingIds));
        conditions.addAll(buildNonFulltextConditions(filter));

        Criteria combined = conditions.stream().reduce(Criteria::and).orElseThrow();
        return executeQuery(Query.query(combined), pageable);
    }

    private Page<Event> executeQuery(Query criteriaQuery, Pageable pageable) {
        Pageable dbPageable = translateDomainToDbColumn(pageable);

        List<Event> results = jdbcAggregateTemplate.findAll(criteriaQuery.with(dbPageable), EventMemento.class)
                .stream()
                .map(EventMemento::toEvent)
                .toList();

        long total = pageable.isUnpaged() ? results.size() : jdbcAggregateTemplate.count(criteriaQuery, EventMemento.class);

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public Set<Integer> findImportedOrisIds(Collection<Integer> candidateOrisIds) {
        if (candidateOrisIds.isEmpty()) {
            return Set.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource("ids", candidateOrisIds);
        List<Integer> found = namedJdbc.query(
                "SELECT oris_id FROM events.events WHERE oris_id IN (:ids)",
                params,
                (rs, rowNum) -> rs.getInt(1)
        );
        return new HashSet<>(found);
    }

    @Override
    public boolean existsByOrisId(int orisId) {
        return jdbcRepository.existsByOrisId(orisId);
    }

    @Override
    public List<Event> findAllUpcomingOrisEvents(LocalDate today) {
        Criteria criteria = Criteria
                .where("status").in(List.of(EventStatus.DRAFT.name(), EventStatus.ACTIVE.name()))
                .and("event_date").greaterThanOrEquals(today)
                .and("oris_id").isNotNull();
        Iterable<EventMemento> mementos = jdbcAggregateTemplate.findAll(Query.query(criteria), EventMemento.class);
        return StreamSupport.stream(mementos.spliterator(), false)
                .map(EventMemento::toEvent)
                .toList();
    }

    /**
     * Builds a Criteria-based query from EventFilter (excluding fulltext — handled separately).
     * Pagination is applied separately so the same query can be reused for counting.
     */
    private Query buildCriteriaQuery(EventFilter filter) {
        List<Criteria> conditions = buildNonFulltextConditions(filter);

        if (conditions.isEmpty()) {
            return Query.empty();
        }

        Criteria combined = conditions.stream().reduce(Criteria::and).orElseThrow();
        return Query.query(combined);
    }

    /**
     * Returns the Criteria conditions for filter dimensions handled by the Spring Data Criteria API.
     * Fulltext and registeredBy cannot be expressed via Criteria (require raw SQL) and are
     * resolved separately via a pre-fetch of matching IDs.
     */
    private List<Criteria> buildNonFulltextConditions(EventFilter filter) {
        List<Criteria> conditions = new ArrayList<>();

        Set<EventStatus> statuses = filter.statuses();
        if (!statuses.isEmpty()) {
            List<String> statusNames = statuses.stream().map(EventStatus::name).toList();
            conditions.add(Criteria.where("status").in(statusNames));
        }

        if (filter.organizer() != null) {
            conditions.add(Criteria.where("organizer").is(filter.organizer()));
        }

        if (filter.dateFrom() != null) {
            conditions.add(Criteria.where("event_date").greaterThanOrEquals(filter.dateFrom()));
        }

        if (filter.dateTo() != null) {
            conditions.add(Criteria.where("event_date").lessThanOrEquals(filter.dateTo()));
        }

        if (!filter.eventTypeIds().isEmpty()) {
            List<UUID> typeIdValues = filter.eventTypeIds().stream()
                    .map(EventTypeId::value)
                    .toList();
            conditions.add(Criteria.where("event_type_id").in(typeIdValues));
        }

        return conditions;
    }

    /**
     * Returns the IDs of events whose {@code name} or {@code location} contains every
     * whitespace-separated token from {@code query}, after stripping diacritics and
     * lowercasing both sides (using the {@code unaccent} function).
     * <p>
     * Each token must match at least one of the two columns (OR across columns).
     * Multiple tokens are ANDed together.
     */
    private List<UUID> findIdsByFulltext(String query) {
        // fulltextQuery is already trimmed and non-blank per EventFilter compact constructor invariant
        String[] tokens = query.split("\\s+");

        StringBuilder sql = new StringBuilder("SELECT id FROM events.events WHERE ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        List<String> tokenClauses = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            String paramName = "token" + i;
            String likePattern = "%" + tokens[i].toLowerCase() + "%";
            params.addValue(paramName, likePattern);
            tokenClauses.add(
                    "unaccent(lower(name)) LIKE unaccent(:" + paramName + ")"
                            + " OR unaccent(lower(coalesce(location, ''))) LIKE unaccent(:" + paramName + ")"
            );
        }

        sql.append(tokenClauses.stream()
                .map(clause -> "(" + clause + ")")
                .collect(Collectors.joining(" AND ")));

        return namedJdbc.query(sql.toString(), params, (rs, rowNum) -> rs.getObject(1, UUID.class));
    }

    /**
     * Returns the IDs of events that have a registration for the given member.
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

    /**
     * Returns IDs of events where the given member is listed as a coordinator.
     * Uses the event_coordinators join table — coordinator membership is an ordered
     * collection so we use EXISTS against the join table, not a column comparison.
     */
    private List<UUID> findIdsByCoordinator(MemberId memberId) {
        String sql = """
                SELECT id FROM events.events e
                WHERE EXISTS (
                    SELECT 1 FROM events.event_coordinators c
                    WHERE c.event_id = e.id AND c.member_id = :memberId
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("memberId", memberId.uuid());
        return namedJdbc.query(sql, params, (rs, rowNum) -> rs.getObject(1, UUID.class));
    }

    /**
     * Returns IDs of events where the given member does NOT have a registration.
     * <p>
     * Scalability note: this scans all events rows. For members with few registrations the
     * result set can be large; in future consider adding a status/event_date pre-filter in SQL
     * rather than intersecting the large list in Java.
     */
    private List<UUID> findIdsByNotRegisteredMember(MemberId memberId) {
        String sql = """
                SELECT id FROM events.events e
                WHERE NOT EXISTS (
                    SELECT 1 FROM events.event_registrations er
                    WHERE er.event_id = e.id AND er.member_id = :memberId
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("memberId", memberId.uuid());
        return namedJdbc.query(sql, params, (rs, rowNum) -> rs.getObject(1, UUID.class));
    }
}
