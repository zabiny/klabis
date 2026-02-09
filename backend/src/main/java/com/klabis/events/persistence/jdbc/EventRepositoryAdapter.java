package com.klabis.events.persistence.jdbc;

import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.EventStatus;
import com.klabis.events.Events;
import com.klabis.events.persistence.EventRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Adapter that bridges between Events public API, EventRepository domain interface and EventJdbcRepository.
 * <p>
 * This adapter implements both:
 * <ul>
 *   <li>{@link Events} - public API for other modules (read-only operations)</li>
 *   <li>{@link com.klabis.events.EventRepository EventRepository} - internal API for events module</li>
 * </ul>
 * <p>
 * It handles conversion between Event entities and EventMemento persistence objects.
 * <p>
 * Event publishing is handled automatically by Spring Modulith via the outbox pattern.
 * The EventMemento delegates @DomainEvents and @AfterDomainEventPublication to the Event entity.
 */
@Component
@Transactional
@SecondaryAdapter
class EventRepositoryAdapter implements Events, EventRepository {

    private final EventJdbcRepository jdbcRepository;

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

    public EventRepositoryAdapter(EventJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    /**
     * Translates Pageable with domain property names to Pageable with database column names.
     */
    private Pageable translateDomainToDbColumn(Pageable pageable) {
        Sort translatedSort = translateSortColumns(pageable.getSort());
        return PageableRequest.of(pageable.getPageNumber(), pageable.getPageSize(), translatedSort);
    }

    /**
     * Translates Sort orders from domain property names to database column names.
     */
    private Sort translateSortColumns(Sort sort) {
        java.util.List<Sort.Order> orders = sort.stream()
                .map(order -> {
                    String dbColumn = DOMAIN_TO_DB_COLUMN.getOrDefault(order.getProperty(), order.getProperty());
                    return new Sort.Order(
                            order.getDirection(),
                            dbColumn,
                            order.getNullHandling()
                    );
                })
                .toList();

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /**
     * Simple Pageable implementation for translated values.
     */
    private static class PageableRequest implements Pageable {
        private final int page;
        private final int size;
        private final Sort sort;

        PageableRequest(int page, int size, Sort sort) {
            this.page = page;
            this.size = size;
            this.sort = sort;
        }

        static Pageable of(int page, int size, Sort sort) {
            return new PageableRequest(page, size, sort);
        }

        @Override
        public int getPageNumber() { return page; }

        @Override
        public int getPageSize() { return size; }

        @Override
        public long getOffset() { return (long) page * size; }

        @Override
        public Sort getSort() { return sort; }

        @Override
        public Pageable next() {
            return new PageableRequest(getPageNumber() + 1, getPageSize(), getSort());
        }

        @Override
        public Pageable previousOrFirst() {
            return getPageNumber() == 0 ? this : new PageableRequest(getPageNumber() - 1, getPageSize(), getSort());
        }

        @Override
        public boolean hasPrevious() {
            return getPageNumber() > 0;
        }

        @Override
        public Pageable first() {
            return new PageableRequest(0, getPageSize(), getSort());
        }

        @Override
        public Pageable withPage(int pageNumber) {
            return new PageableRequest(pageNumber, getPageSize(), getSort());
        }

        public Pageable withSort(Sort sort) {
            return new PageableRequest(getPageNumber(), getPageSize(), sort);
        }

        @Override
        public boolean isPaged() { return true; }

        @Override
        public boolean isUnpaged() { return false; }
    }

    @Override
    public Event save(Event event) {
        // Convert Event to EventMemento for persistence
        EventMemento memento = EventMemento.from(event);
        EventMemento saved = jdbcRepository.save(memento);

        // TODO: Update Event's audit metadata from saved memento (once audit fields are added)

        // Return the same Event instance
        return event;
    }

    @Override
    public Optional<Event> findById(EventId eventId) {
        return jdbcRepository.findById(eventId.value())
                .map(EventMemento::toEvent);
    }

    @Override
    public Page<Event> findAll(Pageable pageable) {
        Pageable dbPageable = translateDomainToDbColumn(pageable);
        return jdbcRepository.findAll(dbPageable)
                .map(EventMemento::toEvent);
    }

    @Override
    public Page<Event> findByStatus(EventStatus status, Pageable pageable) {
        Pageable dbPageable = translateDomainToDbColumn(pageable);
        return jdbcRepository.findByStatus(status.name(), dbPageable)
                .map(EventMemento::toEvent);
    }

    @Override
    public Page<Event> findByOrganizer(String organizer, Pageable pageable) {
        Pageable dbPageable = translateDomainToDbColumn(pageable);
        return jdbcRepository.findByOrganizer(organizer, dbPageable)
                .map(EventMemento::toEvent);
    }

    @Override
    public Page<Event> findByDateRange(LocalDate from, LocalDate to, Pageable pageable) {
        Pageable dbPageable = translateDomainToDbColumn(pageable);
        return jdbcRepository.findByEventDateBetween(from, to, dbPageable)
                .map(EventMemento::toEvent);
    }

    @Override
    public List<Event> findActiveEventsWithDateBefore(LocalDate date) {
        return jdbcRepository.findActiveEventsWithDateBefore(date).stream()
                .map(EventMemento::toEvent)
                .toList();
    }
}
