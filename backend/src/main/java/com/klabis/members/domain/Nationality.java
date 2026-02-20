package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * Value Object representing a nationality as an ISO 3166-1 country code.
 * <p>
 * This value object encapsulates the validation and normalization of nationality codes.
 * Supported formats:
 * - Alpha-2: 2-letter codes (e.g., "CZ", "US", "GB")
 * - Alpha-3: 3-letter codes (e.g., "CZE", "USA", "GBR")
 * <p>
 * All nationality codes are normalized to uppercase for consistency.
 */
@ValueObject
public record Nationality(String code) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 3;
    private static final String ISO_3166_PATTERN = "^[A-Za-z]{2,3}$";

    /**
     * Creates a Nationality value object with validation.
     *
     * @param code the ISO 3166-1 country code (2-3 letters)
     * @throws IllegalArgumentException if the code is null, blank, or not a valid ISO 3166-1 format
     */
    public Nationality {
        Objects.requireNonNull(code, "Nationality code is required");

        String trimmed = code.trim();

        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Nationality code cannot be blank");
        }

        // Validate ISO 3166-1 alpha-2 or alpha-3 format
        if (!trimmed.matches(ISO_3166_PATTERN)) {
            throw new IllegalArgumentException(
                    "Nationality must be a valid ISO 3166-1 code (2-3 letter country code)"
            );
        }

        // Normalize to uppercase
        code = trimmed.toUpperCase();
    }

    /**
     * Static factory method to create a Nationality from a string code.
     *
     * @param code the ISO 3166-1 country code
     * @return Nationality value object
     * @throws IllegalArgumentException if validation fails
     */
    public static Nationality of(String code) {
        return new Nationality(code);
    }

    /**
     * Returns the ISO 3166-1 country code in uppercase.
     *
     * @return the country code (e.g., "CZE", "US")
     */
    @Override
    public String code() {
        return code; // Already normalized in constructor
    }

    /**
     * Checks if this nationality uses an alpha-2 code (2 letters).
     *
     * @return true if the code is 2 letters, false if 3 letters
     */
    public boolean isAlpha2() {
        return code.length() == 2;
    }

    /**
     * Checks if this nationality uses an alpha-3 code (3 letters).
     *
     * @return true if the code is 3 letters, false if 2 letters
     */
    public boolean isAlpha3() {
        return code.length() == 3;
    }

    /**
     * Returns the display name of this nationality (e.g., "CZE" for "Czech Republic").
     * Note: This is a placeholder implementation that returns the code.
     * A full implementation would use a locale/database mapping.
     *
     * @return the country code (placeholder for full name)
     */
    public String displayName() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
