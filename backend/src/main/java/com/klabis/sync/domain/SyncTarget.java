package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

/**
 * Which Klabis entity a synchronisation record belongs to: entity type plus its
 * identifier as an opaque string, so the engine depends on no module's identifier type
 * (design.md D1, Target Domain Model).
 */
@ValueObject
public record SyncTarget(SyncEntityType entityType, String entityId) {

    public SyncTarget {
        Assert.notNull(entityType, "entityType is required");
        Assert.hasText(entityId, "entityId is required");
    }
}
