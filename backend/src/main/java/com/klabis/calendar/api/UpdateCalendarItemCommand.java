package com.klabis.calendar.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Command for updating an existing manual calendar item.
 * <p>
 * All fields are required for updates. For partial updates,
 * the current values should be sent for fields that are not changing.
 * <p>
 * Note: Only manual calendar items (eventId == null) can be updated.
 * Event-linked items are read-only and managed automatically.
 *
 * @param name        calendar item name (required, max 200 characters)
 * @param description calendar item description (required, max 1000 characters)
 * @param startDate   start date (required)
 * @param endDate     end date (required, must be >= startDate)
 */
record UpdateCalendarItemCommand(
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
