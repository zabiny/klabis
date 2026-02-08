package com.klabis.events;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

/**
 * Value Object representing a unique event identifier.
 * <p>
 * This value object wraps a UUID to ensure type safety and provide
 * factory methods for event ID creation.
 * <p>
 * Business rules:
 * - UUID must not be null
 */
@ValueObject
public record EventId(UUID value) implements Identifier {

    /**
     * Creates an EventId value object with validation.
     *
     * @param value UUID for the event (required, must not be null)
     * @throws IllegalArgumentException if validation fails
     */
    public EventId {
        if (value == null) {
            throw new IllegalArgumentException("Event ID is required");
        }
    }

    /**
     * Static factory method to create an EventId from a UUID.
     *
     * @param value UUID for the event
     * @return EventId value object
     * @throws IllegalArgumentException if validation fails
     */
    public static EventId of(UUID value) {
        return new EventId(value);
    }

    /**
     * Generates a new EventId with a random UUID.
     *
     * @return EventId with newly generated UUID
     */
    public static EventId generate() {
        return new EventId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
