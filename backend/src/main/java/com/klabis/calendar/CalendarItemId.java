package com.klabis.calendar;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

/**
 * Value Object representing a unique calendar item identifier.
 * <p>
 * This value object wraps a UUID to ensure type safety and provide
 * factory methods for calendar item ID creation.
 * <p>
 * Business rules:
 * - UUID must not be null
 */
@ValueObject
public record CalendarItemId(UUID value) implements Identifier {

    /**
     * Creates a CalendarItemId value object with validation.
     *
     * @param value UUID for the calendar item (required, must not be null)
     * @throws IllegalArgumentException if validation fails
     */
    public CalendarItemId {
        if (value == null) {
            throw new IllegalArgumentException("Calendar item ID is required");
        }
    }

    /**
     * Static factory method to create a CalendarItemId from a UUID.
     *
     * @param value UUID for the calendar item
     * @return CalendarItemId value object
     * @throws IllegalArgumentException if validation fails
     */
    public static CalendarItemId of(UUID value) {
        return new CalendarItemId(value);
    }

    /**
     * Generates a new CalendarItemId with a random UUID.
     *
     * @return CalendarItemId with newly generated UUID
     */
    public static CalendarItemId generate() {
        return new CalendarItemId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
