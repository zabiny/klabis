package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.pagination.TranslatedPageable;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventStatus;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;


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

    /**
     * Maps domain property names to database column names for sorting.
     */
    private static final java.util.Map<String, String> DOMAIN_TO_DB_COLUMN = java.util.Map.of(
            "eventDate", "event_date",
            "id", "id",
            "name", "name",
            "location", "location",
            "organizer", "organizer",
            "status", "status"
    );

    public EventRepositoryAdapter(EventJdbcRepository jdbcRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
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
        Pageable dbPageable = translateDomainToDbColumn(pageable);
        Query criteriaQuery = buildCriteriaQuery(filter);

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
     * Builds a Criteria-based query from EventFilter without pagination applied.
     * Pagination is applied separately so the same query can be reused for counting.
     */
    private Query buildCriteriaQuery(EventFilter filter) {
        List<Criteria> conditions = new ArrayList<>();

        Set<EventStatus> statuses = filter.statuses();
        if (!statuses.isEmpty()) {
            List<String> statusNames = statuses.stream()
                    .map(EventStatus::name)
                    .toList();
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

        if (conditions.isEmpty()) {
            return Query.empty();
        }

        Criteria combined = conditions.stream()
                .reduce(Criteria::and)
                .orElseThrow();

        return Query.query(combined);
    }
}
