package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.ui.HalForms;
import com.klabis.events.EventId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.MemberId;

import java.math.BigDecimal;
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
 * @param eventTypeId        event type ID (optional); HAL link to /api/event-types/{id} added by postprocessor
 * @param status             event status (DRAFT, ACTIVE, FINISHED, CANCELLED)
 * @param deadlines          registration deadlines in chronological order (max 3)
 * @param ranking            event ranking (optional)
 * @param baseEntryFee       base entry fee (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record EventDto(
        @HalForms(access = HalForms.Access.READ_ONLY) EventId id,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        String websiteUrl,
        MemberId eventCoordinatorId,
        EventTypeId eventTypeId,
        @HalForms(access = HalForms.Access.READ_ONLY) EventStatus status,
        List<String> categories,
        @HalForms(access = HalForms.Access.READ_ONLY) String cancellationReason,
        @HalForms(access = HalForms.Access.READ_ONLY) List<LocalDate> deadlines,
        RankingDto ranking,
        EntryFeeDto baseEntryFee
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RankingDto(String shortName, String name) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record EntryFeeDto(BigDecimal amount, String currency) {}
}
