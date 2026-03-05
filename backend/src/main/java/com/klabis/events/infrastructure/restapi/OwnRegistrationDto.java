package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.HalForms;

import java.time.Instant;

/**
 * DTO for member's own event registration (includes SI card number).
 * <p>
 * This DTO is used when a member views their own registration details.
 * Includes the SI card number which is not visible in the public listing.
 *
 * @param firstName    member's first name
 * @param lastName     member's last name
 * @param siCardNumber member's SI card number for this event
 * @param registeredAt timestamp when registration was created
 */
public record OwnRegistrationDto(
        @HalForms(access = HalForms.Access.READ_ONLY) String firstName,
        @HalForms(access = HalForms.Access.READ_ONLY) String lastName,
        String siCardNumber,
        @HalForms(access = HalForms.Access.READ_ONLY) Instant registeredAt
) {
}
