package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarItemId;
import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for CalendarItem aggregate.
 * <p>
 * Defines persistence operations for the Calendar bounded context.
 * Implementation will be provided in the infrastructure layer.
 * <p>
 * This is internal API for use within the calendar module only.
 *
 * @apiNote Domain repository interface - no Spring Data JDBC dependencies
 */
@SecondaryPort
public interface CalendarRepository {

    /**
     * Saves a calendar item to the repository.
     * <p>
     * For new items, generates audit metadata and persists.
     * For existing items, updates audit metadata and persists.
     *
     * @param calendarItem the calendar item to save
     * @return the saved calendar item with updated audit metadata
     */
    CalendarItem save(CalendarItem calendarItem);

    /**
     * Finds a calendar item by its unique identifier.
     *
     * @param id the calendar item ID
     * @return Optional containing the calendar item if found, empty otherwise
     */
    Optional<CalendarItem> findById(CalendarItemId id);

    /**
     * Finds calendar items that intersect with the specified date range.
     * <p>
     * A calendar item intersects with the range if:
     * - Its start date is on or before the range end date, AND
     * - Its end date is on or after the range start date
     * <p>
     * This query returns multi-day items that span across month boundaries.
     *
     * @param startDate the start date of the range (inclusive)
     * @param endDate   the end date of the range (inclusive)
     * @return list of calendar items intersecting the date range
     */
    List<CalendarItem> findByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Finds a calendar item by linked event ID.
     * <p>
     * Used by event handlers to locate calendar items synchronized from events
     * for update or deletion operations.
     *
     * @param eventId the event ID
     * @return Optional containing the calendar item if found, empty otherwise
     */
    Optional<CalendarItem> findByEventId(EventId eventId);

    /**
     * Deletes a calendar item from the repository.
     * <p>
     * Business rule: Only manual items (eventId == null) should be deleted.
     * Event-linked items are managed automatically via event handlers.
     *
     * @param calendarItem the calendar item to delete
     */
    void delete(CalendarItem calendarItem);
}
