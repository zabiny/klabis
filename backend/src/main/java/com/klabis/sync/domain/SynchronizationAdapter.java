package com.klabis.sync.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

/**
 * The contract an integration implements to plug an entity type and external system
 * into the engine (design.md D3, D16). Published by the {@code sync} module,
 * implemented in the integration's own module (e.g. {@code com.klabis.oris.sync}).
 * <p>
 * The engine drives one record at a time; the adapter only ever handles a single
 * entity (D16 — batching is deliberately postponed).
 */
@SecondaryPort
public interface SynchronizationAdapter {

    SyncEntityType entityType();

    ExternalSystem system();

    SyncCapabilities capabilities();

    /**
     * The concrete {@link SyncProjection} type this adapter produces, so the
     * persistence layer can deserialise a stored projection back into the right type
     * (see {@link SyncProjectionType}) without the {@code sync} module knowing any
     * concrete projection shape itself.
     */
    Class<? extends SyncProjection> projectionType();

    /**
     * Reads the local side and maps it into the canonical projection.
     */
    SyncProjection readLocal(String entityId);

    /**
     * Reads the external side and maps it into the canonical projection.
     */
    SyncProjection readExternal(String externalId);

    /**
     * A cheap external change indicator, when the integration offers one (design.md D3).
     * Adapters without one always return {@link Optional#empty()}, causing the engine
     * to fall back to a full read.
     */
    default Optional<ExternalVersionToken> externalVersion(String externalId) {
        return Optional.empty();
    }

    /**
     * Writes a projection to the local side. Must be idempotent (design.md D12): a
     * full-state update, safe to repeat.
     */
    void applyToLocal(String entityId, SyncProjection projection);

    /**
     * Writes a projection to the external side. Must be idempotent (design.md D12).
     * Only called when {@link SyncCapabilities#writesExternal()} is declared.
     */
    void applyToExternal(String externalId, SyncProjection projection);
}
