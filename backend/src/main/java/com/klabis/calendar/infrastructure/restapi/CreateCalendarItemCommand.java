package com.klabis.calendar.infrastructure.restapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Command for creating a new manual calendar item.
 * <p>
 * Manual calendar items are not linked to events and can be
 * freely updated and deleted by users with CALENDAR:MANAGE authority.
 *
 * @param name        calendar item name (required, max 200 characters)
 * @param description calendar item description (required, max 1000 characters)
 * @param startDate   start date (required)
 * @param endDate     end date (required, must be >= startDate)
 */
record CreateCalendarItemCommand(
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
