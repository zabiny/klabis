package com.klabis.events.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.util.Assert;

import java.util.UUID;

@ValueObject
public record EventCategoryId(UUID value) implements Identifier {

    public EventCategoryId {
        Assert.notNull(value, "Event category ID is required");
    }

    public static EventCategoryId generate() {
        return new EventCategoryId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
