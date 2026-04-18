package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.HalForms;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.MemberId;

import java.time.LocalDate;
import java.util.List;

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
 * @param orisId             ORIS event ID, present only for events imported from ORIS
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
        @HalForms(access = HalForms.Access.READ_ONLY) EventStatus status,
        List<String> categories,
        @HalForms(access = HalForms.Access.READ_ONLY) Integer orisId
) {
}
