package com.klabis.sync.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * What an integration can do for one entity type (design.md D3): which operations
 * exist at all. The engine resolves direction only among the operations that are
 * declared here — a pull-only integration is a first-class, declared fact rather than
 * a method that throws.
 */
@ValueObject
public record SyncCapabilities(
        boolean readsLocal,
        boolean readsExternal,
        boolean writesLocal,
        boolean writesExternal,
        boolean createsLocal,
        boolean createsExternal,
        boolean containsSensitiveData
) {

    /**
     * A pull-only integration (design.md D3, ADR-005): both sides are read, only the
     * local side is written — the external system offers no write for this entity
     * type (ORIS events have no {@code updateEvent}), so an external change is
     * mirrored inward and a local change that cannot be pushed becomes a conflict.
     * Neither side is created by the engine, and the projection carries no sensitive
     * data.
     */
    public static SyncCapabilities pullOnly() {
        return new SyncCapabilities(true, true, true, false, false, false, false);
    }

    /**
     * A genuinely two-way integration (design.md D3, ADR-005): both sides are read
     * and both sides are written, so the engine resolves direction from whichever
     * side changed. Neither side is created by the engine, and the projection carries
     * no sensitive data.
     */
    public static SyncCapabilities bidirectional() {
        return new SyncCapabilities(true, true, true, true, false, false, false);
    }
}
