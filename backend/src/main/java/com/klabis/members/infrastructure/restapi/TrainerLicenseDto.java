package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.TrainerLicense;

import java.time.LocalDate;

/**
 * DTO for trainer license information.
 * <p>
 * This DTO is used to transfer trainer license data between application and presentation layers.
 * It represents the same data as the domain {@link TrainerLicense} value object.
 * <p>
 * Jackson serializes LocalDate in ISO-8601 format (e.g., "2026-01-17").
 *
 * @param licenseNumber trainer license number
 * @param validityDate  validity date of the license (ISO-8601 format)
 */
public record TrainerLicenseDto(
        String licenseNumber,
        LocalDate validityDate
) {
}
