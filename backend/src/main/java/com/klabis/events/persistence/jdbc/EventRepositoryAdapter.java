package com.klabis.events.persistence.jdbc;

import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.EventStatus;
import com.klabis.events.Events;
import com.klabis.events.persistence.EventRepository;
import com.klabis.common.pagination.TranslatedPageable;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return TranslatedPageable.translate(pageable, DOMAIN_TO_DB_COLUMN);
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
