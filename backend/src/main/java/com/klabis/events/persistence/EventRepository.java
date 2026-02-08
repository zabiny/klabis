package com.klabis.events.persistence;

import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.EventStatus;
import com.klabis.events.Events;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Internal repository interface for Event aggregate.
 * <p>
 * Defines persistence operations for the Events bounded context.
 * Implementation will be provided in the infrastructure layer.
 *
 * @apiNote This is internal API for use within the events module only.
 * Other modules should use {@link Events} interface for querying events.
 */
@Repository
@SecondaryPort
public interface EventRepository {

    /**
     * Saves an event to the repository.
     *
     * @param event the event to save
     * @return the saved event with generated ID
     */
    Event save(Event event);

    /**
     * Retrieves an event by its unique identifier.
     *
     * @param eventId the unique identifier of the event
     * @return an {@code Optional} containing the event if found, or an empty {@code Optional} if no such event exists
     */
    Optional<Event> findById(EventId eventId);

    /**
     * Retrieves a page of events from the repository with pagination and sorting.
     *
     * @param pageable the pagination and sorting parameters including page number, page size, and sort criteria
     * @return a page of events containing the requested page data, total element count, and page metadata
     */
    Page<Event> findAll(Pageable pageable);

    /**
     * Retrieves a page of events filtered by status.
     *
     * @param status   the event status to filter by
     * @param pageable the pagination and sorting parameters
     * @return a page of events matching the specified status
     */
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    /**
     * Retrieves a page of events filtered by organizer.
     *
     * @param organizer the organizer name to filter by
     * @param pageable  the pagination and sorting parameters
     * @return a page of events organized by the specified organizer
     */
    Page<Event> findByOrganizer(String organizer, Pageable pageable);

    /**
     * Retrieves a page of events filtered by date range.
     * <p>
     * Returns events where eventDate is between the specified from and to dates (inclusive).
     *
     * @param from     the start date of the range (inclusive)
     * @param to       the end date of the range (inclusive)
     * @param pageable the pagination and sorting parameters
     * @return a page of events within the specified date range
     */
    Page<Event> findByDateRange(LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Finds all active events with event date before the specified date.
     * <p>
     * Used by the automatic event completion scheduler to find events that should
     * be transitioned from ACTIVE to FINISHED status after their event date has passed.
     *
     * @param date the date to compare against event dates
     * @return list of active events with eventDate before the specified date
     */
    List<Event> findActiveEventsWithDateBefore(LocalDate date);
}
