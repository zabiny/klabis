package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * Value Object representing a postal address.
 * <p>
 * This value object encapsulates address information with validation.
 * All fields are trimmed of leading/trailing whitespace for consistency.
 * <p>
 * Business rules:
 * - Street is required and cannot be blank (max 200 characters)
 * - City is required and cannot be blank (max 100 characters)
 * - Postal code is required and cannot be blank (max 20 characters, alphanumeric with optional hyphen/space)
 * - Country is required
 */
@ValueObject
public record Address(String street, String city, String postalCode, Country country) {

    private static final int MAX_STREET_LENGTH = 200;
    private static final int MAX_CITY_LENGTH = 100;
    private static final int MAX_POSTAL_CODE_LENGTH = 20;
    public static final String POSTAL_CODE_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 -]*[A-Za-z0-9]$|^[A-Za-z0-9]$";

    /**
     * Creates an Address value object with validation.
     *
     * @param street     street address (required, not blank, max 200 characters)
     * @param city       city name (required, not blank, max 100 characters)
     * @param postalCode postal code (required, not blank, max 20 characters, alphanumeric with optional hyphen/space)
     * @param country    country (required)
     * @throws IllegalArgumentException if validation fails
     */
    public Address {
        // Validate street
        if (street == null || street.trim().isBlank()) {
            throw new IllegalArgumentException("Street is required");
        }
        street = street.trim();
        if (street.length() > MAX_STREET_LENGTH) {
            throw new IllegalArgumentException("Street must not exceed " + MAX_STREET_LENGTH + " characters");
        }

        // Validate city
        if (city == null || city.trim().isBlank()) {
            throw new IllegalArgumentException("City is required");
        }
        city = city.trim();
        if (city.length() > MAX_CITY_LENGTH) {
            throw new IllegalArgumentException("City must not exceed " + MAX_CITY_LENGTH + " characters");
        }

        // Validate postal code
        if (postalCode == null || postalCode.trim().isBlank()) {
            throw new IllegalArgumentException("Postal code is required");
        }
        postalCode = postalCode.trim();
        if (postalCode.length() > MAX_POSTAL_CODE_LENGTH) {
            throw new IllegalArgumentException("Postal code must not exceed " + MAX_POSTAL_CODE_LENGTH + " characters");
        }
        if (!postalCode.matches(POSTAL_CODE_PATTERN)) {
            throw new IllegalArgumentException("Postal code must be alphanumeric (hyphens and spaces allowed)");
        }

        Assert.notNull(country, "Country is required");
    }

    /**
     * Static factory method to create an Address from street, city, postal code, and country.
     *
     * @param street     street address
     * @param city       city name
     * @param postalCode postal code
     * @param country    country
     * @return Address value object
     * @throws IllegalArgumentException if validation fails
     */
    public static Address of(String street, String city, String postalCode, Country country) {
        return new Address(street, city, postalCode, country);
    }

    @Override
    public String toString() {
        return "Address{" +
               "street='" + street + '\'' +
               ", city='" + city + '\'' +
               ", postalCode='" + postalCode + '\'' +
               ", country='" + country + '\'' +
               '}';
    }
}
