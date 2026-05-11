package com.klabis.events;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record EventTypeId(UUID value) implements Identifier {

    public EventTypeId {
        if (value == null) {
            throw new IllegalArgumentException("EventType ID is required");
        }
    }

    public static EventTypeId of(UUID value) {
        return new EventTypeId(value);
    }

    public static EventTypeId generate() {
        return new EventTypeId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
