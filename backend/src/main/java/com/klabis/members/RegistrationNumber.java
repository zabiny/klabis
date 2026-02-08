package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object representing a member's registration number.
 * <p>
 * Format: XXXYYDD
 * - XXX: Club code (3 alphanumeric characters)
 * - YY: Birth year (last 2 digits)
 * - DD: Sequential number (01-99)
 * <p>
 * Examples: ZBM0501, ABC9915
 * <p>
 * Immutable value object with format validation.
 */
@ValueObject
public final class RegistrationNumber {

    private static final Pattern REGISTRATION_NUMBER_PATTERN =
            Pattern.compile("^[A-Z0-9]{3}\\d{4}$");

    private final String value;

    public static boolean isRegistrationNumber(String value) {
        if (value == null) {
            return false;
        }
        return REGISTRATION_NUMBER_PATTERN.matcher(value.trim()).matches();
    }

    /**
     * Creates a new RegistrationNumber.
     *
     * @param value registration number string in format XXXYYDD
     * @throws IllegalArgumentException if value is invalid
     */
    public RegistrationNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Registration number is required");
        }

        // Normalize to uppercase
        String normalized = value.trim().toUpperCase();

        // Validate format
        if (!isRegistrationNumber(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid registration number format. Expected XXXYYDD where XXX is club code, " +
                    "YY is birth year, and DD is sequence number"
            );
        }

        this.value = normalized;
    }

    /**
     * Factory method to create a new RegistrationNumber.
     *
     * @param value registration number string in format XXXYYDD
     * @return new RegistrationNumber instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static RegistrationNumber of(String value) {
        return new RegistrationNumber(value);
    }

    /**
     * Gets the registration number value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Extracts the club code (first 3 characters).
     */
    public String getClubCode() {
        return value.substring(0, 3);
    }

    /**
     * Extracts the birth year (2 digits).
     */
    public int getBirthYear() {
        return Integer.parseInt(value.substring(3, 5));
    }

    /**
     * Extracts the sequence number (last 2 digits).
     */
    public int getSequenceNumber() {
        return Integer.parseInt(value.substring(5, 7));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegistrationNumber that = (RegistrationNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
