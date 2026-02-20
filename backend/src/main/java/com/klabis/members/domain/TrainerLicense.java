package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

/**
 * Value Object representing a trainer license.
 * <p>
 * This value object encapsulates trainer license information with validation.
 * It delegates validation to the generic {@link ExpiringDocument} value object.
 * <p>
 * Business rules:
 * - License number is required and cannot be blank (max 50 characters)
 * - Validity date is required and cannot be in the past
 */
@ValueObject
public record TrainerLicense(String licenseNumber, LocalDate validityDate) {

    /**
     * Creates a TrainerLicense value object with validation.
     * <p>
     * The validation is delegated to {@link ExpiringDocument}.
     *
     * @param licenseNumber trainer license number (required, not blank, max 50 characters)
     * @param validityDate  validity date of the license (required, not in the past)
     * @throws IllegalArgumentException if validation fails
     */
    public TrainerLicense {
        // Delegate validation to ExpiringDocument
        ExpiringDocument.of(DocumentType.TRAINER_LICENSE, licenseNumber, validityDate);
    }

    /**
     * Static factory method to create a TrainerLicense from license number and validity date.
     *
     * @param licenseNumber trainer license number
     * @param validityDate  validity date of the license
     * @return TrainerLicense value object
     * @throws IllegalArgumentException if validation fails
     */
    public static TrainerLicense of(String licenseNumber, LocalDate validityDate) {
        return new TrainerLicense(licenseNumber, validityDate);
    }
}
