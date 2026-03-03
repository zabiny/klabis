package com.klabis.calendar.application;

import java.time.LocalDate;
import java.util.UUID;

public record CalendarItemDto(
        UUID id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        UUID eventId
) {
}
