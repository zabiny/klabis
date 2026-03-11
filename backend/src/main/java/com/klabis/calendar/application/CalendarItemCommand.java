package com.klabis.calendar.application;

import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@RecordBuilder
public record CalendarItemCommand(
        @NotBlank(message = "Calendar item name is required")
        @Size(max = 200, message = "Calendar item name must not exceed 200 characters")
        String name,

        @NotBlank(message = "Calendar item description is required")
        @Size(max = 1000, message = "Calendar item description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate
) {
}
