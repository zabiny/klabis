package com.klabis.events.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.LocalDate;
import java.util.List;

/**
 * Internal repository interface for Event aggregate.
 * <p>
 * Defines persistence operations for the Events bounded context.
 * Implementation will be provided in the infrastructure layer.
 * <p>
 * Extends {@link Events} public API with write operations (save).
 *
 * @apiNote This is internal API for use within the events module only.
 * Other modules should use {@link Events} interface for querying events.
 */
@SecondaryPort
public interface EventRepository extends Events {

    /**
     * Saves an event to the repository.
     *
     * @param event the event to save
     * @return the saved event with generated ID
     */
    Event save(Event event);

    /**
     * Finds all active events with event date before the specified date.
     * <p>
     * Used by the automatic event completion scheduler to find events that should
     * be transitioned from ACTIVE to FINISHED status after their event date has passed.
     * <p>
     * This is an internal method not exposed in the public Events API.
     *
     * @param date the date to compare against event dates
     * @return list of active events with eventDate before the specified date
     */
    List<Event> findActiveEventsWithDateBefore(LocalDate date);

    // Read methods inherited from Events public API:
    // - Optional<Event> findById(EventId eventId)
    // - Page<Event> findAll(EventFilter filter, Pageable pageable)
}
