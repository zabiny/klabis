package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarFilter;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SecondaryAdapter
@Repository
class CalendarRepositoryAdapter implements CalendarRepository {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.asc("start_date"), Sort.Order.asc("name"));

    private static final Map<String, String> DOMAIN_TO_DB_COLUMN = Map.of(
            "startDate", "start_date",
            "endDate", "end_date",
            "name", "name",
            "kind", "kind",
            "eventId", "event_id"
    );

    private final CalendarJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    CalendarRepositoryAdapter(CalendarJdbcRepository jdbcRepository,
                               JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public CalendarItem save(CalendarItem calendarItem) {
        CalendarMemento saved = jdbcRepository.save(CalendarMemento.from(calendarItem));
        return saved.toCalendarItem();
    }

    @Override
    public Optional<CalendarItem> findById(CalendarItemId id) {
        return jdbcRepository.findById(id.value())
                .map(CalendarMemento::toCalendarItem);
    }

    @Override
    public List<CalendarItem> findByFilter(CalendarFilter filter, Sort sort) {
        Criteria criteria = buildDateRangeCriteria(filter);

        if (!filter.itemTypes().isEmpty()) {
            criteria = criteria.and("kind").in(toKindStrings(filter.itemTypes()));
        }

        if (!filter.eventIds().isEmpty()) {
            criteria = criteria.and("event_id").in(toUuids(filter.eventIds()));
        }

        Sort effectiveSort = sort.isUnsorted() ? DEFAULT_SORT : withNameTiebreaker(translateSort(sort));
        Query query = Query.query(criteria).sort(effectiveSort);

        return jdbcAggregateTemplate.findAll(query, CalendarMemento.class).stream()
                .map(CalendarMemento::toCalendarItem)
                .toList();
    }

    @Override
    public List<CalendarItem> findByEventId(EventId eventId) {
        Criteria criteria = Criteria.where("event_id").is(eventId.value());
        return jdbcAggregateTemplate.findAll(Query.query(criteria), CalendarMemento.class).stream()
                .map(CalendarMemento::toCalendarItem)
                .toList();
    }

    @Override
    public void delete(CalendarItem calendarItem) {
        jdbcRepository.deleteById(calendarItem.getId().value());
    }

    private static Criteria buildDateRangeCriteria(CalendarFilter filter) {
        return Criteria.where("start_date").lessThanOrEquals(filter.endDate())
                .and("end_date").greaterThanOrEquals(filter.startDate());
    }

    private static Sort translateSort(Sort sort) {
        List<Sort.Order> orders = sort.stream()
                .map(order -> new Sort.Order(
                        order.getDirection(),
                        DOMAIN_TO_DB_COLUMN.getOrDefault(order.getProperty(), order.getProperty()),
                        order.getNullHandling()))
                .toList();
        return Sort.by(orders);
    }

    /**
     * Appends {@code name ASC} as a stable tiebreaker when it is not already present,
     * preserving the same secondary ordering the previous hard-coded SQL provided.
     */
    private static Sort withNameTiebreaker(Sort sort) {
        boolean hasName = sort.stream().anyMatch(o -> "name".equals(o.getProperty()));
        return hasName ? sort : sort.and(Sort.by(Sort.Order.asc("name")));
    }

    private static Set<String> toKindStrings(Set<CalendarItemKind> kinds) {
        return kinds.stream().map(CalendarItemKind::name).collect(Collectors.toSet());
    }

    private static Set<UUID> toUuids(Set<EventId> eventIds) {
        return eventIds.stream().map(EventId::value).collect(Collectors.toSet());
    }
}
