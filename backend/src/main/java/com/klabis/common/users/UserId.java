package com.klabis.common.users;

import com.fasterxml.jackson.annotation.JsonValue;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

/**
 * Value object representing a unique user identifier.
 * <p>
 * This value object is shared between the User and Member aggregates to ensure
 * that a Member's ID is always the same as the corresponding User's ID.
 * </p>
 */
@ValueObject
public record UserId(UUID uuid) implements Identifier {

    /**
     * Compact constructor that validates the UUID is not null.
     *
     * @param uuid the unique identifier, must not be null
     * @throws IllegalArgumentException if uuid is null
     */
    public UserId {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
    }

    /**
     * Creates a UserId from a string representation of a UUID.
     *
     * @param uuidString the string representation of the UUID, must not be null or blank
     * @return a new UserId instance
     * @throws IllegalArgumentException if uuidString is null, blank, or not a valid UUID
     */
    public static UserId fromString(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new IllegalArgumentException("UUID string cannot be null or blank");
        }
        try {
            return new UserId(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID string: " + uuidString, e);
        }
    }

    /**
     * Gets the UUID value.
     *
     * @return the UUID value
     */
    @JsonValue
    @Override
    public UUID uuid() {
        return uuid;
    }
}
