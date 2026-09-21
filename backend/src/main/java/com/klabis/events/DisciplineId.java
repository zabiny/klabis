package com.klabis.events;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record DisciplineId(UUID value) implements Identifier {

    public DisciplineId {
        if (value == null) {
            throw new IllegalArgumentException("Discipline ID is required");
        }
    }

    public static DisciplineId of(UUID value) {
        return new DisciplineId(value);
    }

    public static DisciplineId generate() {
        return new DisciplineId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
