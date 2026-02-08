package com.klabis.events;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Value Object representing a SportIdent (SI) card number.
 * <p>
 * This value object encapsulates SI card number validation.
 * The value is trimmed of leading/trailing whitespace for consistency.
 * <p>
 * Business rules:
 * - SI card number must contain only digits
 * - SI card number must be between 4 and 8 digits long
 * - SI card number cannot be null or blank
 */
@ValueObject
public record SiCardNumber(String value) {

    private static final String DIGITS_ONLY_PATTERN = "^\\d+$";
    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 8;

    /**
     * Creates a SiCardNumber value object with validation.
     *
     * @param value SI card number (required, 4-8 digits only)
     * @throws IllegalArgumentException if validation fails
     */
    public SiCardNumber {
        // Validate and trim
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("SI card number is required");
        }
        String trimmedValue = value.trim();

        // Must contain only digits
        if (!trimmedValue.matches(DIGITS_ONLY_PATTERN)) {
            throw new IllegalArgumentException("SI card number must contain only digits");
        }

        // Must be between 4 and 8 digits
        int length = trimmedValue.length();
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException("SI card number must be between 4 and 8 digits");
        }

        value = trimmedValue;
    }

    /**
     * Static factory method to create a SiCardNumber from a string.
     *
     * @param value SI card number string
     * @return SiCardNumber value object
     * @throws IllegalArgumentException if validation fails
     */
    public static SiCardNumber of(String value) {
        return new SiCardNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
