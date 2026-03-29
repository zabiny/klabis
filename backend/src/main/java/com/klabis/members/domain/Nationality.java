package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * Value Object representing a nationality as an ISO 3166-1 alpha-2 country code.
 * <p>
 * Only 2-letter ISO 3166-1 alpha-2 codes are accepted (e.g., "CZ", "US", "GB").
 * All nationality codes are normalized to uppercase for consistency.
 */
@ValueObject
public record Nationality(String code) {

    private static final String ISO_3166_PATTERN = "^[A-Za-z]{2}$";

    /**
     * Creates a Nationality value object with validation.
     *
     * @param code the ISO 3166-1 alpha-2 country code (2 letters)
     * @throws IllegalArgumentException if the code is null, blank, or not a valid ISO 3166-1 alpha-2 format
     */
    public Nationality {
        Objects.requireNonNull(code, "Nationality code is required");

        String trimmed = code.trim();

        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Nationality code cannot be blank");
        }

        if (!trimmed.matches(ISO_3166_PATTERN)) {
            throw new IllegalArgumentException(
                    "Nationality must be a valid ISO 3166-1 alpha-2 code (2-letter country code)"
            );
        }

        // Normalize to uppercase
        code = trimmed.toUpperCase();
    }

    /**
     * Static factory method to create a Nationality from a string code.
     *
     * @param code the ISO 3166-1 alpha-2 country code
     * @return Nationality value object
     * @throws IllegalArgumentException if validation fails
     */
    public static Nationality of(String code) {
        return new Nationality(code);
    }

    /**
     * Returns the ISO 3166-1 alpha-2 country code in uppercase.
     *
     * @return the 2-letter country code (e.g., "CZ", "US")
     */
    @Override
    public String code() {
        return code; // Already normalized in constructor
    }

    /**
     * Checks if this nationality is Czech (CZ).
     *
     * @return true if the nationality code is "CZ"
     */
    public boolean isCzech() {
        return "CZ".equals(code);
    }

    /**
     * Returns the display name of this nationality.
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
