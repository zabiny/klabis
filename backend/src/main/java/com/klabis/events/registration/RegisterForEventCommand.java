package com.klabis.events.registration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Command for registering a member for an event.
 *
 * @param siCardNumber SI card number (6-7 digits, required)
 */
record RegisterForEventCommand(
        @NotBlank(message = "SI card number is required")
        @Pattern(regexp = "\\d{6,7}", message = "SI card number must be 6-7 digits")
        String siCardNumber
) {
}
