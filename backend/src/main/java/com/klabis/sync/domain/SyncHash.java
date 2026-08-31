package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * The digest of a whole {@link SyncProjection} — never a per-field digest
 * (design.md D13). Stored in plaintext alongside the encrypted projection column.
 */
@ValueObject
public record SyncHash(String value) {

    public SyncHash {
        Assert.hasText(value, "value is required");
    }

    public static SyncHash of(String value) {
        return new SyncHash(value);
    }
}
