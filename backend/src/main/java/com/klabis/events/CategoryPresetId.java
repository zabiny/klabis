package com.klabis.events;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record CategoryPresetId(UUID value) implements Identifier {

    public CategoryPresetId {
        if (value == null) {
            throw new IllegalArgumentException("CategoryPreset ID is required");
        }
    }

    public static CategoryPresetId of(UUID value) {
        return new CategoryPresetId(value);
    }

    public static CategoryPresetId generate() {
        return new CategoryPresetId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
