package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncResolution;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * The engine's entry point (design.md, Target Domain Model). Consumed by the REST
 * layer, the scheduler and integrations.
 * <p>
 * {@link #enroll}, {@link #synchronizeNow}, {@link #state} and {@link #retire} cover
 * ordinary passes; {@link #acknowledgeConflict} and {@link #resolveConflict} cover the
 * two-step conflict resolution workflow (design.md D6, D7). Reset is added by a later
 * slice on this same port.
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

    /**
     * A manager confirms they have seen the current collision (design.md D7). The
     * server binds the acknowledgement to the record's present hash pair; only once
     * acknowledged does {@link #resolveConflict} accept a call for this specific
     * collision.
     *
     * @param actingUser opaque identifier of the acknowledging user (design.md D15)
     * @throws SyncRecordNotFoundException      if the entity is not enrolled
     * @throws SyncRecordNotInConflictException if the record is not currently in conflict
     */
    SyncRecord acknowledgeConflict(SyncRecordId id, String actingUser);

    /**
     * Resolves a standing, acknowledged conflict (design.md D6, D7): re-reads both
     * sides through the adapter first and proceeds only if the fresh hash pair still
     * equals the one acknowledged. A direction the adapter cannot perform is refused.
     *
     * @param actingUser opaque identifier of the resolving user (design.md D15)
     * @throws SyncRecordNotFoundException        if the entity is not enrolled
     * @throws SyncRecordNotInConflictException   if the record is not currently in conflict
     * @throws ConflictNotAcknowledgedException   if there is no acknowledgement, or it no longer matches
     *                                             the fresh hash pair (the record's snapshots are refreshed
     *                                             from the fresh reads and the conflict is left standing)
     * @throws UnsupportedResolutionException     if the requested direction is not supported by the integration
     */
    SyncRecord resolveConflict(SyncRecordId id, SyncResolution resolution, String actingUser);
}
