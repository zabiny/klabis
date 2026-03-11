package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

/**
 * Generic Value Object representing an expiring document.
 * <p>
 * This value object encapsulates common document information with validation.
 * It is designed to be type-safe through the use of {@link DocumentType} enum parameter.
 * <p>
 * Business rules:
 * - Document number is required and cannot be blank (max 50 characters)
 * - Validity date is required
 * <p>
 * Example usage:
 * <pre>{@code
 * // Create an identity card document
 * ExpiringDocument identityCard = ExpiringDocument.of(
 *     DocumentType.IDENTITY_CARD,
 *     "12345678",
 *     LocalDate.of(2026, 12, 31)
 * );
 *
 * // Create a trainer license document
 * ExpiringDocument trainerLicense = ExpiringDocument.of(
 *     DocumentType.TRAINER_LICENSE,
 *     "TL-2024-001",
 *     LocalDate.of(2025, 6, 30)
 * );
 * }</pre>
 *
 * @param <T>          the document type enum parameter
 * @param documentType type of the document
 * @param number       document number
 * @param validityDate date when the document expires
 */
@ValueObject
public record ExpiringDocument<T extends DocumentType>(
        T documentType,
        String number,
        LocalDate validityDate
) {

    private static final int MAX_DOCUMENT_NUMBER_LENGTH = 50;

    /**
     * Creates an ExpiringDocument value object with validation.
     *
     * @param documentType type of the document
     * @param number       document number (required, not blank, max 50 characters)
     * @param validityDate validity date of the document (required)
     * @throws IllegalArgumentException if validation fails
     */
    public ExpiringDocument {
        String documentTypeName = documentType != null ? documentType.getDisplayName() : "Document";

        // Validate document number
        if (number == null || number.trim().isBlank()) {
            throw new IllegalArgumentException(documentTypeName + " number is required");
        }
        number = number.trim();
        if (number.length() > MAX_DOCUMENT_NUMBER_LENGTH) {
            throw new IllegalArgumentException(documentTypeName + " number must not exceed " + MAX_DOCUMENT_NUMBER_LENGTH + " characters");
        }

        // Validate validity date
        if (validityDate == null) {
            throw new IllegalArgumentException(documentTypeName + " validity date is required");
        }

    }

    /**
     * Static factory method to create an ExpiringDocument.
     *
     * @param documentType type of the document
     * @param number       document number
     * @param validityDate validity date of the document
     * @param <T>          the document type enum parameter
     * @return ExpiringDocument value object
     * @throws IllegalArgumentException if validation fails
     */
    public static <T extends DocumentType> ExpiringDocument<T> of(T documentType, String number, LocalDate validityDate) {
        return new ExpiringDocument<>(documentType, number, validityDate);
    }
}
