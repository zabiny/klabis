package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Value Object representing a phone number in E.164 format.
 * <p>
 * This value object encapsulates phone number validation.
 * The phone value is trimmed of leading/trailing whitespace for consistency.
 * <p>
 * Business rules:
 * - Phone number must be in E.164 format (starts with +)
 * - Phone number must contain only digits and spaces (after the +)
 * - Phone number must contain at least one digit
 */
@ValueObject
public record PhoneNumber(String value) {

    private static final String E164_PREFIX = "+";
    private static final String DIGITS_AND_SPACES_PATTERN = "^[0-9\\s]+$";
    private static final String AT_LEAST_ONE_DIGIT_PATTERN = ".*\\d.*";

    /**
     * Creates a PhoneNumber value object with validation.
     *
     * @param value phone number in E.164 format (required, starts with +, digits and spaces only)
     * @throws IllegalArgumentException if validation fails
     */
    public PhoneNumber {
        // Validate and trim
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        String trimmedValue = value.trim();

        // Must start with +
        if (!trimmedValue.startsWith(E164_PREFIX)) {
            throw new IllegalArgumentException("Phone number must start with + (E.164 format)");
        }

        // Remove the + for further validation
        String withoutPlus = trimmedValue.substring(1);

        // Must contain at least one digit
        if (!withoutPlus.matches(AT_LEAST_ONE_DIGIT_PATTERN)) {
            throw new IllegalArgumentException("Phone number must contain at least one digit");
        }

        // Must contain only digits and spaces
        if (!withoutPlus.matches(DIGITS_AND_SPACES_PATTERN)) {
            throw new IllegalArgumentException("Phone number must contain only digits and spaces");
        }

        value = trimmedValue;
    }

    /**
     * Static factory method to create a PhoneNumber from a string.
     *
     * @param value phone number in E.164 format
     * @return PhoneNumber value object
     * @throws IllegalArgumentException if validation fails
     */
    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
