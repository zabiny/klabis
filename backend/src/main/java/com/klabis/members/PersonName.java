package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * Value Object representing a person's name.
 * <p>
 * This value object encapsulates a person's first and last name with validation.
 * All names are trimmed of leading/trailing whitespace for consistency.
 * <p>
 * Business rules:
 * - First name is required and cannot be blank
 * - Last name is required and cannot be blank
 */
@ValueObject
public record PersonName(String firstName, String lastName) {

    /**
     * Creates a PersonName value object with validation.
     *
     * @param firstName person's first name (required, not blank)
     * @param lastName  person's last name (required, not blank)
     * @throws IllegalArgumentException if validation fails
     */
    public PersonName {
        Assert.hasLength(firstName, "First name is required");
        Assert.hasLength(lastName, "Last name is required");

        String trimmedFirstName = firstName.trim();
        String trimmedLastName = lastName.trim();

        if (trimmedFirstName.isBlank()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }

        if (trimmedLastName.isBlank()) {
            throw new IllegalArgumentException("Last name cannot be blank");
        }

        // Use trimmed values
        firstName = trimmedFirstName;
        lastName = trimmedLastName;
    }

    /**
     * Static factory method to create a PersonName from first and last name.
     *
     * @param firstName person's first name
     * @param lastName  person's last name
     * @return PersonName value object
     * @throws IllegalArgumentException if validation fails
     */
    public static PersonName of(String firstName, String lastName) {
        return new PersonName(firstName, lastName);
    }

    /**
     * Returns the full name as "firstName lastName".
     * Handles multiple spaces between names.
     *
     * @return full name with single space between first and last name
     */
    public String fullName() {
        return (firstName + " " + lastName).replaceAll("\\s+", " ").trim();
    }

    /**
     * Returns the initials (first letter of first and last name).
     *
     * @return initials in uppercase (e.g., "JN" for John Novak)
     */
    public String initials() {
        return (firstName.charAt(0) + "" + lastName.charAt(0)).toUpperCase();
    }

    /**
     * Returns a formal name as "LastName, FirstName".
     *
     * @return formal name format
     */
    public String formalName() {
        return lastName + ", " + firstName;
    }

    @Override
    public String toString() {
        return fullName();
    }
}
