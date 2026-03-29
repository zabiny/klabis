package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.HalForms;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.MemberId;

import java.time.LocalDate;

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
        @HalForms(access = HalForms.Access.READ_ONLY) EventId id,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        String websiteUrl,
        MemberId eventCoordinatorId,
        LocalDate registrationDeadline,
        @HalForms(access = HalForms.Access.READ_ONLY) EventStatus status
) {
}
