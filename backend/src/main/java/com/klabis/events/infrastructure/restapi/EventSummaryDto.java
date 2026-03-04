package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.HalForms;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventStatus;

import java.time.LocalDate;

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
        @HalForms(access = HalForms.Access.READ_ONLY) EventId id,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        @HalForms(access = HalForms.Access.READ_ONLY) EventStatus status
) {
}
