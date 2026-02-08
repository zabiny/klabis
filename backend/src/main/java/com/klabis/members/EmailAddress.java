package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Value Object representing an email address.
 * <p>
 * This value object encapsulates email address validation.
 * The email value is trimmed of leading/trailing whitespace for consistency.
 * <p>
 * Business rules:
 * - Email must contain @ symbol
 * - Email must have both local part and domain
 * - Follows basic RFC 5322 format validation (simplified)
 */
@ValueObject
public record EmailAddress(String value) {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /**
     * Creates an EmailAddress value object with validation.
     *
     * @param value email address (required, must contain @ and domain)
     * @throws IllegalArgumentException if validation fails
     */
    public EmailAddress {
        // Validate and trim
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("Email address is required");
        }
        String trimmedValue = value.trim();

        // Basic email validation: must contain @ and have content before and after it
        int atIndex = trimmedValue.indexOf('@');
        if (atIndex == -1 || atIndex == 0 || atIndex == trimmedValue.length() - 1) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Validate against basic email pattern
        if (!trimmedValue.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        value = trimmedValue;
    }

    /**
     * Static factory method to create an EmailAddress from a string.
     *
     * @param value email address
     * @return EmailAddress value object
     * @throws IllegalArgumentException if validation fails
     */
    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
