package com.klabis.events.domain;

import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.Port;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Public query API for Event aggregate.
 * <p>
 * Provides read-only access to events for other modules.
 * This is the only public interface that should be used by external modules
 * to query event information.
 *
 * @see com.klabis.events.EventRepository
 */
@Port
public interface Events {

    /**
     * Find an event by its unique ID.
     *
     * @param eventId the unique identifier of the event
     * @return optional containing the event if found, or empty otherwise
     */
    Optional<Event> findById(EventId eventId);

    /**
     * Find events matching the given filter criteria, with pagination and sorting.
     *
     * @param filter   the criteria to filter events by; use {@link EventFilter#none()} for unfiltered results
     * @param pageable the pagination and sorting parameters
     * @return page of events matching the filter
     */
    Page<Event> findAll(EventFilter filter, Pageable pageable);
}
