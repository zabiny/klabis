package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.EventStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for event details response.
 * <p>
 * Contains complete event information for display purposes.
 *
 * @param id                 event unique identifier
 * @param name               event name
 * @param eventDate          event date
 * @param location           event location
 * @param organizer          event organizer
 * @param websiteUrl         event website URL (optional)
 * @param eventCoordinatorId event coordinator user ID (optional)
 * @param status             event status (DRAFT, ACTIVE, FINISHED, CANCELLED)
 */
record EventDto(
        UUID id,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        String websiteUrl,
        UUID eventCoordinatorId,
        EventStatus status
) {
}
