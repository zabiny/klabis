package com.klabis.events.management;

import com.klabis.events.EventStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for event summary in list responses.
 * <p>
 * Contains essential event information for list views.
 *
 * @param id        event unique identifier
 * @param name      event name
 * @param eventDate event date
 * @param location  event location
 * @param organizer event organizer
 * @param status    event status (DRAFT, ACTIVE, FINISHED, CANCELLED)
 */
record EventSummaryDto(
        UUID id,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        EventStatus status
) {
}
