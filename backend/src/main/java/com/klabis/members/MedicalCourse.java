package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Value Object representing a medical course completion.
 * <p>
 * This value object encapsulates medical course information with validation.
 * Some medical courses have indefinite validity, while others expire after a certain period.
 * <p>
 * Business rules:
 * - Completion date is required
 * - Validity date is optional, but if provided must be after completion date
 */
@ValueObject
public record MedicalCourse(LocalDate completionDate, Optional<LocalDate> validityDate) {

    /**
     * Creates a MedicalCourse value object with validation.
     *
     * @param completionDate course completion date (required)
     * @param validityDate   course validity date (optional, if provided must be after completion date)
     * @throws IllegalArgumentException if validation fails
     */
    public MedicalCourse {
        Assert.notNull(completionDate, "Medical course completion date");

        // Rule: Methods returning Optional must never return null
        // Replace null with Optional.empty() to ensure consistency
        if (validityDate == null) {
            validityDate = Optional.empty();
        }

        // Validate validity date if provided
        validityDate.ifPresent(validity -> Assert.state(validity.isAfter(completionDate),
                "Medical course validity date"));
    }

    /**
     * Static factory method to create a MedicalCourse.
     *
     * @param completionDate course completion date (required)
     * @param validityDate   course validity date (optional, if provided must be after completion date)
     * @return MedicalCourse value object
     * @throws IllegalArgumentException if validation fails
     */
    public static MedicalCourse of(LocalDate completionDate, Optional<LocalDate> validityDate) {
        return new MedicalCourse(completionDate, validityDate);
    }
}
