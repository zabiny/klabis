package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * One pairing found by {@link SyncRecordRepository#findByExternalReferences} — both
 * sides of the pairing rather than just the external id, since a caller matching
 * candidates against results needs the external id back, and a caller wanting to jump
 * straight to the paired Klabis entity gets it for free instead of a second round trip
 * (design.md D1).
 */
@ValueObject
public record SyncedEntityReference(SyncTarget target, ExternalReference externalReference) {

    public SyncedEntityReference {
        Assert.notNull(target, "target is required");
        Assert.notNull(externalReference, "externalReference is required");
    }
}
