package com.klabis.members.application;

import com.klabis.members.domain.IdentityCard;

import java.time.LocalDate;

/**
 * DTO for identity card information.
 * <p>
 * This DTO is used to transfer identity card data between application and presentation layers.
 * It represents the same data as the domain {@link IdentityCard} value object.
 * <p>
 * Jackson serializes LocalDate in ISO-8601 format (e.g., "2026-01-17").
 *
 * @param cardNumber   identity card number
 * @param validityDate validity date of the card (ISO-8601 format)
 */
public record IdentityCardDto(
        String cardNumber,
        LocalDate validityDate
) {
}
