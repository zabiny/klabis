package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.RefereeLicense;
import com.klabis.members.domain.RefereeLevel;

import java.time.LocalDate;

/**
 * DTO for referee license information.
 * <p>
 * This DTO is used to transfer referee license data between application and presentation layers.
 * It represents the same data as the domain {@link RefereeLicense} value object.
 * <p>
 * Jackson serializes LocalDate in ISO-8601 format (e.g., "2026-01-17").
 *
 * @param level        referee license level (R1, R2, R3)
 * @param validityDate validity date of the license (ISO-8601 format)
 */
public record RefereeLicenseDto(
        RefereeLevel level,
        LocalDate validityDate
) {
}
