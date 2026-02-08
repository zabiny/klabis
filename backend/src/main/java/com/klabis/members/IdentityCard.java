package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

/**
 * Value Object representing an identity card.
 * <p>
 * This value object encapsulates identity card information with validation.
 * It delegates validation to the generic {@link ExpiringDocument} value object.
 * <p>
 * Business rules:
 * - Card number is required and cannot be blank (max 50 characters)
 * - Validity date is required and cannot be in the past
 */
@ValueObject
public record IdentityCard(String cardNumber, LocalDate validityDate) {

    /**
     * Creates an IdentityCard value object with validation.
     * <p>
     * The validation is delegated to {@link ExpiringDocument}.
     *
     * @param cardNumber   identity card number (required, not blank, max 50 characters)
     * @param validityDate validity date of the card (required, not in the past)
     * @throws IllegalArgumentException if validation fails
     */
    public IdentityCard {
        // Delegate validation to ExpiringDocument
        ExpiringDocument.of(DocumentType.IDENTITY_CARD, cardNumber, validityDate);
    }

    /**
     * Static factory method to create an IdentityCard from card number and validity date.
     *
     * @param cardNumber   identity card number
     * @param validityDate validity date of the card
     * @return IdentityCard value object
     * @throws IllegalArgumentException if validation fails
     */
    public static IdentityCard of(String cardNumber, LocalDate validityDate) {
        return new IdentityCard(cardNumber, validityDate);
    }
}
