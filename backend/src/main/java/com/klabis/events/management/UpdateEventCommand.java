package com.klabis.events.management;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Command for updating an existing event.
 * <p>
 * All fields are required for updates. For partial updates,
 * the current values should be sent for fields that are not changing.
 *
 * @param name               event name (required, max 100 characters)
 * @param eventDate          event date (required)
 * @param location           event location (required, max 100 characters)
 * @param organizer          event organizer (required, max 10 characters)
 * @param websiteUrl         event website URL (optional, must be valid URL)
 * @param eventCoordinatorId event coordinator user ID (optional)
 */
record UpdateEventCommand(
        @NotBlank(message = "Event name is required")
        @Size(max = 100, message = "Event name must not exceed 100 characters")
        String name,

        @NotNull(message = "Event date is required")
        LocalDate eventDate,

        @NotBlank(message = "Event location is required")
        @Size(max = 100, message = "Event location must not exceed 100 characters")
        String location,

        @NotBlank(message = "Event organizer is required")
        @Size(max = 10, message = "Event organizer must not exceed 10 characters")
        String organizer,

        @URL(message = "Website URL must be valid")
        String websiteUrl,

        UUID eventCoordinatorId
) {
}
