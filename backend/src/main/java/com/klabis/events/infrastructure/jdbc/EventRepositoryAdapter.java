package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.pagination.TranslatedPageable;
import com.klabis.events.EventId;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


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
            "status", "status"
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
     * Returns the intersection of IDs that satisfy the fulltext and/or registeredBy constraints,
     * or null if neither constraint is active (meaning no pre-filtering is needed).
     */
    private List<UUID> resolvePreFilteredIds(EventFilter filter) {
        List<UUID> fulltextIds = filter.fulltextQuery() != null
                ? findIdsByFulltext(filter.fulltextQuery())
                : null;

        List<UUID> registeredByIds = filter.registeredBy() != null
                ? findIdsByRegisteredMember(filter.registeredBy())
                : null;

        if (fulltextIds == null && registeredByIds == null) {
            return null;
        }
        if (fulltextIds == null) {
            return registeredByIds;
        }
        if (registeredByIds == null) {
            return fulltextIds;
        }

        Set<UUID> registeredBySet = Set.copyOf(registeredByIds);
        return fulltextIds.stream().filter(registeredBySet::contains).toList();
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
    public boolean existsByOrisId(int orisId) {
        return jdbcRepository.existsByOrisId(orisId);
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

        if (filter.coordinator() != null) {
            conditions.add(Criteria.where("event_coordinator_id").is(filter.coordinator().uuid()));
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

        StringBuilder sql = new StringBuilder("SELECT id FROM events WHERE ");
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
                SELECT id FROM events e
                WHERE EXISTS (
                    SELECT 1 FROM event_registrations er
                    WHERE er.event_id = e.id AND er.member_id = :memberId
                )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("memberId", memberId.uuid());
        return namedJdbc.query(sql, params, (rs, rowNum) -> rs.getObject(1, UUID.class));
    }
}
