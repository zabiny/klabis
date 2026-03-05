package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.CalendarItemId;
import com.klabis.common.ui.HalForms;
import com.klabis.events.EventId;

import java.time.LocalDate;

public record CalendarItemDto(
        @HalForms(access = HalForms.Access.READ_ONLY) CalendarItemId id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        @HalForms(access = HalForms.Access.READ_ONLY) EventId eventId
) {
}
