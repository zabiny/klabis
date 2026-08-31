package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * The engine's entry point (design.md, Target Domain Model). Consumed by the REST
 * layer, the scheduler and integrations.
 * <p>
 * Slice 1 implements {@link #enroll}, {@link #synchronizeNow}, {@link #state} and
 * {@link #retire}. Conflict acknowledgement/resolution and reset are added by later
 * slices on this same port.
 */
@PrimaryPort
public interface SynchronizationPort {

    /**
     * Enrols an entity for synchronisation against an external system (design.md D17).
     *
     * @throws UnknownSyncEntityTypeException if no adapter is registered for the target's entity type
     */
    SyncRecord enroll(SyncTarget target, ExternalReference externalReference);

    /**
     * Runs one synchronisation pass for this record immediately (design.md D9, "How a
     * pass runs").
     *
     * @param actingUser opaque identifier of the user who triggered this pass, taken
     *                    from the authenticated principal by the caller (design.md
     *                    D15) — the application layer receives it explicitly rather
     *                    than reading the security context itself. {@code null} for a
     *                    caller with no authenticated user (e.g. the scheduler).
     * @throws SyncRecordNotFoundException if the entity is not enrolled
     */
    SyncRecord synchronizeNow(SyncRecordId id, String actingUser);

    /**
     * Reads the current synchronisation state without running a pass.
     *
     * @throws SyncRecordNotFoundException if the entity is not enrolled
     */
    SyncRecord state(SyncRecordId id);

    /**
     * Retires the record: no longer scanned, kept with its history intact (design.md D17).
     *
     * @throws SyncRecordNotFoundException if the entity is not enrolled
     */
    void retire(SyncRecordId id);
}
