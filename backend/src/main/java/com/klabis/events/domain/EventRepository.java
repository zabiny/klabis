package com.klabis.events.domain;

import java.time.LocalDate;
import java.util.List;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

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
     * Checks if an event with the given ORIS ID already exists.
     * Used to prevent duplicate imports from ORIS.
     *
     * @param orisId the ORIS event identifier
     * @return true if an event with this orisId already exists
     */
    boolean existsByOrisId(int orisId);

    /**
     * Returns all DRAFT or ACTIVE events whose event date is on or after {@code today}
     * and which have an ORIS event ID — i.e., events eligible for bulk sync.
     *
     * @param today lower-bound for event date (inclusive)
     * @return unordered list of matching events
     */
    List<Event> findAllUpcomingOrisEvents(LocalDate today);

    // Read methods inherited from Events public API:
    // - Optional<Event> findById(EventId id)
    // - Page<Event> findAll(EventFilter filter, Pageable pageable)
}
