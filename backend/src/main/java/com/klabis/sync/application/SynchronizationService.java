package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Orchestrates one synchronisation pass (design.md D9, "How a pass runs"): the
 * version-token short-circuit, reading both sides, comparing against the baseline,
 * adopting the external side on first enrolment, writing inward or outward depending
 * on which side changed, rebasing the baseline on convergence, and appending the
 * attempt to history. Both inward and outward writes re-read the local projection
 * immediately before writing and abort if it moved since the decision was made
 * (design.md D9) — the concurrent-edit guard.
 * <p>
 * Conflicts, retry/claim handling and scheduling are added by later slices around
 * this same orchestration.
 */
@Service
class SynchronizationService implements SynchronizationPort {

    private final SyncRecordRepository syncRecordRepository;
    private final SyncAttemptRepository syncAttemptRepository;
    private final SynchronizationAdapterRegistry adapterRegistry;
    private final SyncProjectionHasher hasher;
    private final ConflictSnapshotRefresher conflictSnapshotRefresher;

    SynchronizationService(
            SyncRecordRepository syncRecordRepository,
            SyncAttemptRepository syncAttemptRepository,
            SynchronizationAdapterRegistry adapterRegistry,
            SyncProjectionHasher hasher,
            ConflictSnapshotRefresher conflictSnapshotRefresher
    ) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncAttemptRepository = syncAttemptRepository;
        this.adapterRegistry = adapterRegistry;
        this.hasher = hasher;
        this.conflictSnapshotRefresher = conflictSnapshotRefresher;
    }

    @Transactional
    @Override
    public SyncRecord enroll(SyncTarget target, ExternalReference externalReference) {
        adapterRegistry.find(target.entityType(), externalReference.system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(target.entityType(), externalReference.system()));

        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target, externalReference);
        return syncRecordRepository.save(record);
    }

    @Transactional
    @Override
    public SyncRecord synchronizeNow(SyncRecordId id, String actingUser) {
        SyncRecord record = getOrThrow(id);
        SynchronizationAdapter adapter = adapterRegistry.find(record.getTarget().entityType(), record.getExternalReference().system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(record.getTarget().entityType(), record.getExternalReference().system()));

        return runPass(record, adapter, SyncTriggerKind.MANUAL, actingUser);
    }

    @Transactional(readOnly = true)
    @Override
    public SyncRecord state(SyncRecordId id) {
        return getOrThrow(id);
    }

    @Transactional
    @Override
    public void retire(SyncRecordId id) {
        SyncRecord record = getOrThrow(id);
        record.retire();
        syncRecordRepository.save(record);
    }

    @Transactional
    @Override
    public SyncRecord acknowledgeConflict(SyncRecordId id, String actingUser) {
        SyncRecord record = getOrThrow(id);
        requireConflict(record);

        ConflictAcknowledgement acknowledgement = new ConflictAcknowledgement(
                record.getLocal().hash(), record.getExternal().hash(), Instant.now(), actingUser);
        record.acknowledgeConflict(acknowledgement);

        SyncRecord saved = syncRecordRepository.save(record);
        appendAttempt(saved, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS,
                saved.getLocal().hash(), saved.getExternal().hash(), null, actingUser);
        return saved;
    }

    /**
     * Resolves a standing, acknowledged conflict (design.md D6, D7). A resolution
     * never trusts stored snapshots: both sides are re-read through the adapter first,
     * and the call proceeds only if the fresh hash pair still equals the acknowledged
     * one. If a side moved in between, the record's snapshots are refreshed from the
     * fresh reads via {@link ConflictSnapshotRefresher} — a separate bean whose
     * {@code REQUIRES_NEW} transaction commits independently of this method's own, so
     * the refresh survives the {@link ConflictNotAcknowledgedException} this method
     * goes on to throw — the conflict is left standing (re-raised so a subsequent GET
     * shows the new collision), and the call is rejected.
     * <p>
     * This method's own transaction still spans the external re-reads and the
     * eventual write (design.md D12 is not honoured here — that full transaction-
     * boundary revisit is Slice 4's job), so that the happy path's record save and
     * attempt append commit atomically: D15 requires every attempt to appear in the
     * history, and a crash between two separately-committed writes would leave a
     * resolved record with no attempt row to show for it.
     */
    @Transactional
    @Override
    public SyncRecord resolveConflict(SyncRecordId id, SyncResolution resolution, String actingUser) {
        SyncRecord record = getOrThrow(id);
        requireConflict(record);

        if (!record.isAcknowledgementCurrent()) {
            throw new ConflictNotAcknowledgedException(id);
        }

        SynchronizationAdapter adapter = adapterRegistry.find(record.getTarget().entityType(), record.getExternalReference().system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(record.getTarget().entityType(), record.getExternalReference().system()));

        if (resolution == SyncResolution.OUTWARD && !adapter.capabilities().writesExternal()) {
            throw new UnsupportedResolutionException(id, resolution);
        }
        if (resolution == SyncResolution.INWARD && !adapter.capabilities().writesLocal()) {
            throw new UnsupportedResolutionException(id, resolution);
        }

        String entityId = record.getTarget().entityId();
        String externalId = record.getExternalReference().externalId();
        SyncSnapshot freshLocal = SyncSnapshot.of(adapter.readLocal(entityId), hasher);
        SyncSnapshot freshExternal = SyncSnapshot.of(adapter.readExternal(externalId), hasher);

        if (!record.getAcknowledgement().isCurrentFor(freshLocal.hash(), freshExternal.hash())) {
            // A side moved since the acknowledgement (design.md D7): refresh the
            // record's snapshots from the fresh reads so a subsequent GET shows the new
            // collision, but write nothing and leave the conflict standing. Committed in
            // its own REQUIRES_NEW transaction (see ConflictSnapshotRefresher) so this
            // method's own rollback, about to be triggered by the throw below, does not
            // undo it.
            conflictSnapshotRefresher.refresh(id, freshLocal, freshExternal);
            throw new ConflictNotAcknowledgedException(id);
        }

        SyncRecord resolved = applyResolution(record, adapter, resolution, freshLocal, freshExternal);

        appendAttempt(resolved, SyncTriggerKind.MANUAL, resolutionDirection(resolution), SyncOutcome.SUCCESS,
                freshLocal.hash(), freshExternal.hash(), null, actingUser);
        return resolved;
    }

    private SyncRecord applyResolution(SyncRecord record, SynchronizationAdapter adapter, SyncResolution resolution, SyncSnapshot freshLocal, SyncSnapshot freshExternal) {
        return switch (resolution) {
            case INWARD -> resolveInward(record, adapter, freshExternal);
            case OUTWARD -> resolveOutward(record, adapter, freshLocal);
            case ACCEPT_DIVERGENCE -> {
                record.acceptDivergence(freshLocal, freshExternal);
                yield syncRecordRepository.save(record);
            }
        };
    }

    private static SyncDirection resolutionDirection(SyncResolution resolution) {
        return switch (resolution) {
            case INWARD -> SyncDirection.INWARD;
            case OUTWARD -> SyncDirection.OUTWARD;
            case ACCEPT_DIVERGENCE -> null;
        };
    }

    /**
     * A forced {@code INWARD} resolution follows the same post-write re-read rule as
     * any other inward write (design.md D9): the fresh external projection is written
     * to the local side, then the local side is re-read again, and that post-write
     * state becomes both the local snapshot and the baseline.
     */
    private SyncRecord resolveInward(SyncRecord record, SynchronizationAdapter adapter, SyncSnapshot freshExternal) {
        String entityId = record.getTarget().entityId();
        adapter.applyToLocal(entityId, freshExternal.projection());
        SyncSnapshot postWriteLocal = SyncSnapshot.of(adapter.readLocal(entityId), hasher);
        record.resolveWithDirection(SyncDirection.INWARD, postWriteLocal, freshExternal);
        return syncRecordRepository.save(record);
    }

    private SyncRecord resolveOutward(SyncRecord record, SynchronizationAdapter adapter, SyncSnapshot freshLocal) {
        String externalId = record.getExternalReference().externalId();
        adapter.applyToExternal(externalId, freshLocal.projection());
        record.resolveWithDirection(SyncDirection.OUTWARD, freshLocal, freshLocal);
        return syncRecordRepository.save(record);
    }

    private void requireConflict(SyncRecord record) {
        if (record.getStatus() != SyncStatus.CONFLICT) {
            throw new SyncRecordNotInConflictException(record.getId());
        }
    }

    private SyncRecord getOrThrow(SyncRecordId id) {
        return syncRecordRepository.findById(id).orElseThrow(() -> new SyncRecordNotFoundException(id));
    }

    /**
     * Runs one pass: version-token short-circuit, reads, comparison, write, and
     * appending the attempt to history (design.md D9, D3).
     */
    private SyncRecord runPass(SyncRecord record, SynchronizationAdapter adapter, SyncTriggerKind trigger, String actingUser) {
        String entityId = record.getTarget().entityId();
        String externalId = record.getExternalReference().externalId();

        if (record.getBaseline() != null && record.getDirtySince() == null) {
            Optional<ExternalVersionToken> currentToken = adapter.externalVersion(externalId);
            if (currentToken.isPresent() && currentToken.get().equals(record.getExternalVersion())) {
                // Cheap change indicator unchanged and no local edit observed: skip the
                // full read entirely (design.md D3). Still recorded as an attempt — D15
                // requires every attempt to appends a row, including this one.
                appendAttempt(record, trigger, null, SyncOutcome.SKIPPED,
                        record.getLocal() != null ? record.getLocal().hash() : null,
                        record.getExternal() != null ? record.getExternal().hash() : null,
                        null, actingUser);
                return record;
            }
        }

        SyncProjection localProjection = adapter.readLocal(entityId);
        SyncProjection externalProjection = adapter.readExternal(externalId);
        SyncSnapshot currentLocal = SyncSnapshot.of(localProjection, hasher);
        SyncSnapshot currentExternal = SyncSnapshot.of(externalProjection, hasher);

        SyncDecision decision = record.decide(currentLocal, currentExternal, adapter.capabilities());

        SyncHash localHashForAttempt = currentLocal.hash();
        SyncHash externalHashForAttempt = currentExternal.hash();

        switch (decision.kind()) {
            case NOTHING_TO_DO -> {
                appendAttempt(record, trigger, null, SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                return record;
            }
            case CONVERGED -> {
                // Both sides changed independently to the same value: rebase both
                // baselines, write nothing (design.md D4).
                record.recordConverged(currentLocal);
                SyncRecord savedConverged = syncRecordRepository.save(record);
                appendAttempt(savedConverged, trigger, null, SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                return savedConverged;
            }
            case CONFLICT -> {
                // Neither side is written while a conflict stands (design.md D6, D7).
                // decide() already resolved which direction (if any) was attempted —
                // WRITE's direction when the only blocker was a missing outward write
                // capability, otherwise none for a genuine two-sided or divergence-guard
                // conflict.
                record.recordConflict(currentLocal, currentExternal, decision.direction());
                SyncRecord savedConflict = syncRecordRepository.save(record);
                appendAttempt(savedConflict, trigger, decision.direction(), SyncOutcome.CONFLICT, localHashForAttempt, externalHashForAttempt, null, actingUser);
                return savedConflict;
            }
            case ADOPT_EXTERNAL, WRITE -> {
                boolean written = decision.direction() == SyncDirection.INWARD
                        ? writeInward(record, adapter, currentExternal, currentLocal)
                        : writeOutward(record, adapter, currentLocal);
                if (!written) {
                    // The local side moved again between the decision read and the
                    // write (design.md D9): abort, leave the record due for the next
                    // pass, and record the attempt as a no-op rather than a success. An
                    // inward abort leaves the record untouched; an outward abort still
                    // rebases the external side and the whole baseline onto what was
                    // actually pushed (recordOutwardWriteWithSkippedAdvance), so the
                    // next pass sees only the newer local edit as due, not the engine's
                    // own write — persist that either way.
                    SyncRecord savedAfterAbort = syncRecordRepository.save(record);
                    SyncHash externalHashAfterAbort = savedAfterAbort.getExternal() != null
                            ? savedAfterAbort.getExternal().hash() : externalHashForAttempt;
                    appendAttempt(savedAfterAbort, trigger, decision.direction(), SyncOutcome.SKIPPED, localHashForAttempt, externalHashAfterAbort, null, actingUser);
                    return savedAfterAbort;
                }
            }
        }

        adapter.externalVersion(externalId).ifPresent(record::setExternalVersion);
        SyncRecord saved = syncRecordRepository.save(record);
        appendAttempt(saved, trigger, decision.direction(), SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
        return saved;
    }

    /**
     * Writes the external projection to the local side, then re-reads the local
     * projection so the post-write state becomes both the local snapshot and the
     * baseline (design.md D9) — never the pushed value itself, since the write may
     * have been transformed (e.g. field-ownership merges) on the way in.
     * <p>
     * Immediately before the write, the local projection is re-read; if its hash no
     * longer matches the one the direction decision was based on, the write is
     * aborted and this returns {@code false} so the record stays due.
     *
     * @return {@code true} if the write happened, {@code false} if aborted
     */
    private boolean writeInward(SyncRecord record, SynchronizationAdapter adapter, SyncSnapshot currentExternal, SyncSnapshot decisionLocal) {
        String entityId = record.getTarget().entityId();

        SyncSnapshot freshLocal = SyncSnapshot.of(adapter.readLocal(entityId), hasher);
        if (!freshLocal.matches(decisionLocal)) {
            return false;
        }

        adapter.applyToLocal(entityId, currentExternal.projection());

        SyncProjection postWriteLocal = adapter.readLocal(entityId);
        SyncSnapshot postWriteSnapshot = SyncSnapshot.of(postWriteLocal, hasher);

        record.recordSuccess(SyncDirection.INWARD, postWriteSnapshot, currentExternal);
        return true;
    }

    /**
     * Writes the local projection to the external side (design.md D9). Immediately
     * before the baseline is written, the local projection is re-read; if it no
     * longer matches what was pushed, this returns {@code false} so the record stays
     * due for the next pass — but the external side, and the whole baseline, still
     * rebase onto what was actually pushed
     * ({@link SyncRecord#recordOutwardWriteWithSkippedAdvance}), since the write to
     * the external system happened regardless and the next pass must not mistake it
     * for an independent external change.
     * <p>
     * On success, the external side now holds what was pushed — {@code currentLocal}'s
     * content — so that (not the stale pre-write {@code currentExternal} snapshot)
     * becomes both the record's external snapshot and the external half of the new
     * baseline.
     *
     * @return {@code true} if the baseline was written, {@code false} if skipped
     */
    private boolean writeOutward(SyncRecord record, SynchronizationAdapter adapter, SyncSnapshot currentLocal) {
        String entityId = record.getTarget().entityId();
        String externalId = record.getExternalReference().externalId();

        adapter.applyToExternal(externalId, currentLocal.projection());

        SyncSnapshot freshLocal = SyncSnapshot.of(adapter.readLocal(entityId), hasher);
        if (!freshLocal.matches(currentLocal)) {
            record.recordOutwardWriteWithSkippedAdvance(currentLocal);
            return false;
        }

        record.recordSuccess(SyncDirection.OUTWARD, currentLocal, currentLocal);
        return true;
    }

    private void appendAttempt(
            SyncRecord record,
            SyncTriggerKind trigger,
            SyncDirection direction,
            SyncOutcome outcome,
            SyncHash localHash,
            SyncHash externalHash,
            String failureReason,
            String actingUser
    ) {
        // Scheduled and event-triggered attempts carry no acting user (design.md D15);
        // only manually triggered work does, and it is passed in by the caller.
        String recordedActingUser = trigger == SyncTriggerKind.MANUAL ? actingUser : null;
        syncAttemptRepository.save(SyncAttempt.record(
                record.getId(), trigger, direction, outcome, localHash, externalHash, failureReason, recordedActingUser));
    }
}
