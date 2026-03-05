package com.klabis.events.infrastructure.restapi;

import java.time.Instant;

/**
 * DTO for event registration (public view without SI card number).
 * <p>
 * This DTO is used for listing registrations to protect member privacy.
 * SI card numbers are considered sensitive and only visible to the member who owns the registration.
 *
 * @param firstName    member's first name
 * @param lastName     member's last name
 * @param registeredAt timestamp when registration was created
 */
public record RegistrationDto(
        String firstName,
        String lastName,
        Instant registeredAt
) {
}
