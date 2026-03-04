package com.klabis.calendar.infrastructure.restapi;

import com.klabis.common.ui.HalForms;

import java.time.LocalDate;
import java.util.UUID;

public record CalendarItemDto(
        @HalForms(access = HalForms.Access.READ_ONLY) UUID id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        @HalForms(access = HalForms.Access.READ_ONLY) UUID eventId
) {
}
