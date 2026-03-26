package com.klabis.events.domain;

import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.Port;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
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
     * Find all events with pagination and sorting.
     *
     * @param pageable the pagination and sorting parameters
     * @return page of events
     */
    Page<Event> findAll(Pageable pageable);

    /**
     * Find events filtered by status.
     *
     * @param status   the event status to filter by
     * @param pageable the pagination and sorting parameters
     * @return page of events matching the specified status
     */
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    /**
     * Find events excluding the specified status.
     *
     * @param excludedStatus the event status to exclude
     * @param pageable       the pagination and sorting parameters
     * @return page of events not matching the excluded status
     */
    Page<Event> findByStatusNot(EventStatus excludedStatus, Pageable pageable);

    /**
     * Find events filtered by date range.
     *
     * @param from     the start date of the range (inclusive)
     * @param to       the end date of the range (inclusive)
     * @param pageable the pagination and sorting parameters
     * @return page of events within the specified date range
     */
    Page<Event> findByDateRange(LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Find events by organizer.
     *
     * @param organizer the organizer name to filter by
     * @param pageable  the pagination and sorting parameters
     * @return page of events organized by the specified organizer
     */
    Page<Event> findByOrganizer(String organizer, Pageable pageable);
}
