package com.klabis.calendar.api;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for calendar item response.
 * <p>
 * Contains complete calendar item information for display purposes.
 * The eventId field indicates if this is a manual item (null) or
 * event-linked item (non-null).
 *
 * @param id          calendar item unique identifier
 * @param name        calendar item name
 * @param description calendar item description
 * @param startDate   start date
 * @param endDate     end date
 * @param eventId     linked event ID (null for manual items)
 */
record CalendarItemDto(
        UUID id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        UUID eventId
) {
}
