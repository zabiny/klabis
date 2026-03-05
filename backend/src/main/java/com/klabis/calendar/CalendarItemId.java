package com.klabis.calendar;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record CalendarItemId(UUID value) implements Identifier {

    public CalendarItemId {
        if (value == null) {
            throw new IllegalArgumentException("Calendar item ID is required");
        }
    }

    public static CalendarItemId of(UUID value) {
        return new CalendarItemId(value);
    }

    public static CalendarItemId generate() {
        return new CalendarItemId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
