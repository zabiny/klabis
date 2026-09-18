package com.klabis.sync;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.util.Assert;

import java.util.UUID;

@ValueObject
public record SyncAttemptId(UUID value) implements Identifier {

    public SyncAttemptId {
        Assert.notNull(value, "value is required");
    }

    public static SyncAttemptId newId() {
        return new SyncAttemptId(UUID.randomUUID());
    }
}
