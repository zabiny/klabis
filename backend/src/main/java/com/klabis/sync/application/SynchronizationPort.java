package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncResolution;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;
import java.util.Optional;

/**
 * The engine's entry point (design.md, Target Domain Model). Consumed by the REST
 * layer, the scheduler and integrations.
 * <p>
 * {@link #enroll}, {@link #synchronizeNow}, {@link #state} and {@link #retire} cover
 * ordinary passes; {@link #acknowledgeConflict} and {@link #resolveConflict} cover the
 * two-step conflict resolution workflow (design.md D6, D7); {@link #reset} restarts a
 * terminally failed record (design.md D10).
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
     * Looks up the synchronisation record for a Klabis entity by target alone — no
     * external system in the call, unlike {@link #enroll} (design.md D14's REST
     * resources are addressed by entity type and id only, since a caller reading or
     * acting on a record does not choose which external system it is paired with).
     * <p>
     * Resolves the external system via every registered adapter for the target's
     * entity type. Ordinarily exactly one; this change's API does not yet handle an
     * entity type paired against more than one external system at once (the
     * {@code sync_record} unique constraint allows it, but no caller needs it yet).
     *
     * @return empty if the entity is not enrolled for synchronisation against any
     * registered adapter
     * @throws AmbiguousSyncTargetException if more than one adapter is registered for
     *                                       the target's entity type — a configuration
     *                                       error, not a normal empty result
     */
    Optional<SyncRecord> findByTarget(SyncTarget target);

    /**
     * Every non-retired synchronisation record for the given entity type — {@code
     * FAILED} included — across every external system it is enrolled against, unlike
     * {@link #findByTarget} which resolves one entity to at most one record. Backs the
     * manual {@code all-upcoming} bulk pass, which must see a {@code CONFLICT} or
     * {@code FAILED} record in order to count and report it as skipped, not just the
     * records it can actually attempt (design.md D18's {@code all-upcoming} semantics —
     * "still honouring... the {@code CONFLICT}/{@code FAILED} skip").
     */
    List<SyncRecord> findActiveByEntityType(SyncEntityType entityType);

    /**
     * Marks the record for this target dirty (design.md D9) — a scheduling signal
     * only, collapsing a burst of local edits into one due pass; never consulted to
     * decide whether a write is safe. Does nothing if the target is not enrolled: a
     * module publishing this signal does not know, and should not need to know,
     * whether synchronisation is active for the entity that changed.
     */
    void markDirty(SyncTarget target);

    /**
     * The number of retryable/terminal attempts already recorded in the record's
     * history since its most recent {@code SUCCESS} or {@code RESET} (design.md D10) —
     * the read-state figure a caller sees on {@code getSyncState} (design.md D14).
     * <p>
     * This does <b>not</b> include an attempt currently in progress: {@code
     * handleFailure}'s own {@code + 1} accounts for the failure being classified right
     * now, before it is appended to history — a distinction this method does not make,
     * since by the time a caller reads state there is no in-flight attempt to count.
     *
     * @throws SyncRecordNotFoundException if the entity is not enrolled
     */
    int failedAttemptsSinceLastSuccess(SyncRecordId id);

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

    /**
     * A manager restarts a terminally failed record (design.md D10): appends a
     * {@code RESET} attempt so the derived failure count restarts, clears the due
     * date, and returns the record to service.
     *
     * @param actingUser opaque identifier of the resetting user (design.md D15)
     * @throws SyncRecordNotFoundException   if the entity is not enrolled
     * @throws SyncRecordNotFailedException  if the record is not currently terminally failed
     */
    SyncRecord reset(SyncRecordId id, String actingUser);
}
