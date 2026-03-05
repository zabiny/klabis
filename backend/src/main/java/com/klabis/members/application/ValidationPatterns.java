package com.klabis.members.application;

/**
 * Centralized validation patterns and messages for consistency across the domain and application layers.
 * <p>
 * This class contains constants for regex patterns and validation messages used throughout
 * the members module to ensure DRY (Don't Repeat Yourself) principle and maintain consistency.
 * <p>
 * All patterns are designed to be used by both domain validators and Jakarta Validation annotations.
 */
public final class ValidationPatterns {

    private ValidationPatterns() {
        // Utility class - prevent instantiation
    }

    // ========== Field Names ==========

    /**
     * Field name for postal code in validation messages.
     */
    public static final String FIELD_POSTAL_CODE = "Postal code";

    /**
     * Field name for country in validation messages.
     */
    public static final String FIELD_COUNTRY = "Country";

    // ========== Length Constraints ==========

    // ========== Regex Patterns ==========

    /**
     * Regex pattern for postal codes.
     * <p>
     * Accepts alphanumeric characters with optional hyphens and spaces.
     * Must start and end with alphanumeric character, or be a single alphanumeric character.
     * Examples: "12345", "AB12 3CD", "123-456", "A1B2C3"
     */
    public static final String POSTAL_CODE_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 -]*[A-Za-z0-9]$|^[A-Za-z0-9]$";

    /**
     * Regex pattern for ISO 3166-1 alpha-2 country codes.
     * <p>
     * Accepts exactly 2 letters (case-insensitive).
     * Examples: "US", "GB", "CA", "DE"
     */
    public static final String ISO_3166_ALPHA_2_PATTERN = "^[A-Za-z]{2}$";

    /**
     * Regex pattern for numeric-only strings.
     * <p>
     * Accepts only digits (0-9). Useful for validating identifiers like chip numbers.
     * Examples: "12345", "00123", "0"
     */
    public static final String NUMERIC_ONLY_PATTERN = "^[0-9]+$";

    // ========== Validation Messages ==========

    /**
     * Error message when postal code format is invalid.
     */
    public static final String MESSAGE_POSTAL_CODE_INVALID = FIELD_POSTAL_CODE + " must be alphanumeric (hyphens and spaces allowed)";

    /**
     * Error message when country code format is invalid.
     */
    public static final String MESSAGE_COUNTRY_INVALID = FIELD_COUNTRY + " must be a valid ISO 3166-1 alpha-2 code (2 letters)";
}
