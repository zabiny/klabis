package com.klabis.sync;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;
import org.springframework.util.Assert;

import java.util.UUID;

/**
 * Identity of a {@link com.klabis.sync.domain.SyncRecord} aggregate. Lives in the
 * module's root package (not {@code domain}) because it is part of the module's
 * public API — used by the primary port and, eventually, the REST layer.
 */
@ValueObject
public record SyncRecordId(UUID value) implements Identifier {

    public SyncRecordId {
        Assert.notNull(value, "value is required");
    }

    public static SyncRecordId newId() {
        return new SyncRecordId(UUID.randomUUID());
    }
}
