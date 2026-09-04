package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.List;
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
 * A pass runs in three phases (design.md D12): {@link SyncRecordClaimer} reads the
 * record and claims it in one short transaction; this class calls the external system
 * through {@link ResilientAdapterExecutor} with no transaction open; {@link
 * SyncOutcomeWriter} persists the outcome (record and attempt, atomically) in a second
 * short transaction. Both collaborators are separate beans — a self-invoked
 * {@code @Transactional} method on this class would silently skip its transaction
 * boundary. Failures are classified by {@link FailureClassifier} and scheduled by
 * {@link RetryScheduler} (design.md D10, D11).
 */
@Service
class SynchronizationService implements SynchronizationPort {

    private final SyncRecordRepository syncRecordRepository;
    private final SyncAttemptRepository syncAttemptRepository;
    private final SynchronizationAdapterRegistry adapterRegistry;
    private final SyncProjectionHasher hasher;
    private final ResilientAdapterExecutor resilientAdapterExecutor;
    private final RetryScheduler retryScheduler;
    private final SyncRecordClaimer claimer;
    private final SyncOutcomeWriter outcomeWriter;

    SynchronizationService(
            SyncRecordRepository syncRecordRepository,
            SyncAttemptRepository syncAttemptRepository,
            SynchronizationAdapterRegistry adapterRegistry,
            SyncProjectionHasher hasher,
            ResilientAdapterExecutor resilientAdapterExecutor,
            SyncProperties properties,
            SyncRecordClaimer claimer,
            SyncOutcomeWriter outcomeWriter
    ) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncAttemptRepository = syncAttemptRepository;
        this.adapterRegistry = adapterRegistry;
        this.hasher = hasher;
        this.resilientAdapterExecutor = resilientAdapterExecutor;
        this.retryScheduler = new RetryScheduler(properties);
        this.claimer = claimer;
        this.outcomeWriter = outcomeWriter;
    }

    @Transactional
    @Override
    public SyncRecord enroll(SyncTarget target, ExternalReference externalReference) {
        adapterRegistry.find(target.entityType(), externalReference.system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(target.entityType(), externalReference.system()));

        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target, externalReference);
        return syncRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    @Override
    public int failedAttemptsSinceLastSuccess(SyncRecordId id) {
        getOrThrow(id);
        return retryScheduler.failedAttemptsSince(syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(id));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<SyncRecord> findByTarget(SyncTarget target) {
        return resolveSoleSystem(target.entityType())
                .flatMap(system -> syncRecordRepository.findByTargetAndSystem(target, system));
    }

    @Transactional(readOnly = true)
    @Override
    public List<SyncRecord> findActiveByEntityType(SyncEntityType entityType) {
        return syncRecordRepository.findAllNonRetired().stream()
                .filter(record -> record.getTarget().entityType() == entityType)
                .toList();
    }

    @Transactional
    @Override
    public void markDirty(SyncTarget target) {
        resolveSoleSystem(target.entityType()).ifPresent(system -> markDirty(target, system));
    }

    private void markDirty(SyncTarget target, ExternalSystem system) {
        syncRecordRepository.findByTargetAndSystem(target, system).ifPresent(record -> {
            record.markDirty();
            // An inward write raises EventUpdatedEvent on the very entity the pass is
            // writing (design.md D9 — "an inward write is itself a local change"), so
            // this listener call can race the same pass's own trailing
            // SyncOutcomeWriter#persist on the same sync_record row. Dirty-since is
            // scheduling, never correctness (design.md D9, D11's "Dirty-since" row): if
            // the concurrent pass's save wins, its own write already raises the flag it
            // needs, so losing this one is a safe no-op, not a lost update.
            try {
                syncRecordRepository.save(record);
            } catch (OptimisticLockingFailureException ignored) {
                // Best effort only — see above.
            }
        });
    }

    @Override
    public SyncRecord synchronizeNow(SyncRecordId id, String actingUser) {
        // A CONFLICT or FAILED record needs a decision — acknowledge/resolve or reset
        // — before a MANUAL trigger may touch it again (design.md D6, D7, D10, and the
        // REST API section's 409 on these two statuses). Without this guard a manual
        // trigger would claim and re-run the normal decision table, silently returning
        // a terminally failed record to service with no RESET attempt row, or writing
        // outside the conflict resolution workflow. A CONFLICT record is still
        // re-evaluated by the scheduled cadences (design.md D7 — "recomputed on each
        // pass") via runScheduledPass; only the manual trigger is refused here.
        SyncStatus statusBeforeClaim = getOrThrow(id).getStatus();
        if (statusBeforeClaim == SyncStatus.CONFLICT || statusBeforeClaim == SyncStatus.FAILED) {
            throw new SyncRecordNeedsResolutionException(id, statusBeforeClaim);
        }

        SyncRecord record = claimer.claim(id);
        SynchronizationAdapter adapter = adapterRegistry.find(record.getTarget().entityType(), record.getExternalReference().system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(record.getTarget().entityType(), record.getExternalReference().system()));

        return runPass(record, adapter, SyncTriggerKind.MANUAL, actingUser);
    }

    /**
     * Runs a scheduled pass for one record (design.md D9, D10): unlike
     * {@link #synchronizeNow}, this is not refused for a {@code CONFLICT} record — a
     * conflict is recomputed on every pass and can clear itself (design.md D7) —
     * though {@code FAILED} and {@code RETIRED} records are still never attempted
     * (design.md D10, D17). No REST-facing exception on the FAILED/RETIRED case: the
     * caller (the scheduler, added in a later slice) is expected to filter its scan to
     * due, non-terminal, non-retired records before calling this at all, so reaching
     * this method for one is a programming error, not user input.
     * <p>
     * Package-private: there is no scheduler yet to call this in production (Slice 5).
     * Exercised directly by tests that verify a conflict clearing itself on
     * re-evaluation (design.md D7), which is not the manual trigger's job.
     */
    SyncRecord runScheduledPass(SyncRecordId id) {
        SyncRecord existing = getOrThrow(id);
        Assert.state(existing.getStatus() != SyncStatus.FAILED && existing.getStatus() != SyncStatus.RETIRED,
                () -> "A scheduled pass must not be run against a record in status " + existing.getStatus());

        SyncRecord record = claimer.claim(id);
        SynchronizationAdapter adapter = adapterRegistry.find(record.getTarget().entityType(), record.getExternalReference().system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(record.getTarget().entityType(), record.getExternalReference().system()));

        return runPass(record, adapter, SyncTriggerKind.SCHEDULED, null);
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
        syncAttemptRepository.save(SyncAttempt.record(saved.getId(), SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS,
                saved.getLocal().hash(), saved.getExternal().hash(), null, actingUser));
        return saved;
    }

    /**
     * Resolves a standing, acknowledged conflict (design.md D6, D7). A resolution
     * never trusts stored snapshots: both sides are re-read through the adapter first,
     * with no transaction open (design.md D12), and the call proceeds only if the
     * fresh hash pair still equals the acknowledged one. If a side moved in between,
     * the record's snapshots are refreshed from the fresh reads and saved directly —
     * this method carries no {@code @Transactional} of its own to roll back, so the
     * refresh commits on its own via the repository's per-call transaction — the
     * conflict is left standing (re-raised so a subsequent GET shows the new
     * collision), and the call is rejected.
     * <p>
     * The record save and attempt append on the happy path commit together in
     * {@link SyncOutcomeWriter#persistResolution}.
     */
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
        SyncSnapshot freshLocal = SyncSnapshot.of(resilientAdapterExecutor.call(() -> adapter.readLocal(entityId)), hasher);
        SyncSnapshot freshExternal = SyncSnapshot.of(resilientAdapterExecutor.call(() -> adapter.readExternal(externalId)), hasher);

        if (!record.getAcknowledgement().isCurrentFor(freshLocal.hash(), freshExternal.hash())) {
            // A side moved since the acknowledgement (design.md D7): refresh the
            // record's snapshots from the fresh reads so a subsequent GET shows the new
            // collision, but write nothing and leave the conflict standing. This method
            // has no ambient transaction to roll back, so the save below commits on its
            // own even though the throw that follows ends the call in failure.
            record.recordConflict(freshLocal, freshExternal, null);
            syncRecordRepository.save(record);
            throw new ConflictNotAcknowledgedException(id);
        }

        SyncRecord written = switch (resolution) {
            case INWARD -> {
                resilientAdapterExecutor.run(() -> adapter.applyToLocal(entityId, freshExternal.projection()));
                SyncSnapshot postWriteLocal = SyncSnapshot.of(resilientAdapterExecutor.call(() -> adapter.readLocal(entityId)), hasher);
                record.resolveWithDirection(SyncDirection.INWARD, postWriteLocal, freshExternal);
                yield record;
            }
            case OUTWARD -> {
                resilientAdapterExecutor.run(() -> adapter.applyToExternal(externalId, freshLocal.projection()));
                record.resolveWithDirection(SyncDirection.OUTWARD, freshLocal, freshLocal);
                yield record;
            }
            case ACCEPT_DIVERGENCE -> {
                record.acceptDivergence(freshLocal, freshExternal);
                yield record;
            }
        };

        return outcomeWriter.persistResolution(written, resolutionDirection(resolution), freshLocal.hash(), freshExternal.hash(), actingUser);
    }

    @Override
    public SyncRecord reset(SyncRecordId id, String actingUser) {
        SyncRecord record = getOrThrow(id);
        if (record.getStatus() != SyncStatus.FAILED) {
            throw new SyncRecordNotFailedException(id);
        }
        record.reset();
        return outcomeWriter.persist(record, SyncTriggerKind.MANUAL, null, SyncOutcome.RESET, null, null, null, actingUser);
    }

    private static SyncDirection resolutionDirection(SyncResolution resolution) {
        return switch (resolution) {
            case INWARD -> SyncDirection.INWARD;
            case OUTWARD -> SyncDirection.OUTWARD;
            case ACCEPT_DIVERGENCE -> null;
        };
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
     * Resolves the single external system registered for an entity type (design.md
     * D14 — one adapter per entity type in this change). Empty when nothing is
     * registered; throws when more than one adapter claims the same entity type,
     * since callers have no way to pick between them.
     */
    private Optional<ExternalSystem> resolveSoleSystem(SyncEntityType entityType) {
        List<ExternalSystem> systems = adapterRegistry.systemsFor(entityType);
        if (systems.isEmpty()) {
            return Optional.empty();
        }
        if (systems.size() > 1) {
            throw new AmbiguousSyncTargetException(entityType, systems.size());
        }
        return Optional.of(systems.get(0));
    }

    /**
     * Runs one pass (design.md D9, D3, D10, D11): the version-token short-circuit,
     * reads, comparison, write, failure classification, and appending the attempt to
     * history. The external calls happen with no transaction open; only the final
     * persist (record and attempt, atomically, via {@link SyncOutcomeWriter}) is
     * transactional.
     */
    private SyncRecord runPass(SyncRecord record, SynchronizationAdapter adapter, SyncTriggerKind trigger, String actingUser) {
        String entityId = record.getTarget().entityId();
        String externalId = record.getExternalReference().externalId();

        try {
            if (record.getBaseline() != null && record.getDirtySince() == null) {
                Optional<ExternalVersionToken> currentToken = resilientAdapterExecutor.call(() -> adapter.externalVersion(externalId));
                if (currentToken.isPresent() && currentToken.get().equals(record.getExternalVersion())) {
                    // Cheap change indicator unchanged and no local edit observed: skip
                    // the full read entirely (design.md D3). Still recorded as an
                    // attempt — D15 requires every attempt to appends a row.
                    return outcomeWriter.persist(record, trigger, null, SyncOutcome.SKIPPED,
                            record.getLocal() != null ? record.getLocal().hash() : null,
                            record.getExternal() != null ? record.getExternal().hash() : null,
                            null, actingUser);
                }
            }

            SyncProjection localProjection = resilientAdapterExecutor.call(() -> adapter.readLocal(entityId));
            SyncProjection externalProjection = resilientAdapterExecutor.call(() -> adapter.readExternal(externalId));
            SyncSnapshot currentLocal = SyncSnapshot.of(localProjection, hasher);
            SyncSnapshot currentExternal = SyncSnapshot.of(externalProjection, hasher);

            SyncDecision decision = record.decide(currentLocal, currentExternal, adapter.capabilities());

            SyncHash localHashForAttempt = currentLocal.hash();
            SyncHash externalHashForAttempt = currentExternal.hash();

            return switch (decision.kind()) {
                case NOTHING_TO_DO -> outcomeWriter.persist(record, trigger, null, SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                case CONVERGED -> {
                    // Both sides changed independently to the same value: rebase both
                    // baselines, write nothing (design.md D4).
                    record.recordConverged(currentLocal);
                    yield outcomeWriter.persist(record, trigger, null, SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                }
                case CONFLICT -> {
                    // Neither side is written while a conflict stands (design.md D6, D7).
                    record.recordConflict(currentLocal, currentExternal, decision.direction());
                    yield outcomeWriter.persist(record, trigger, decision.direction(), SyncOutcome.CONFLICT, localHashForAttempt, externalHashForAttempt, null, actingUser);
                }
                case ADOPT_EXTERNAL, WRITE -> {
                    boolean written = decision.direction() == SyncDirection.INWARD
                            ? writeInward(record, adapter, currentExternal, currentLocal)
                            : writeOutward(record, adapter, currentLocal);
                    if (!written) {
                        // The local side moved again between the decision read and the
                        // write (design.md D9): abort, leave the record due for the next
                        // pass, and record the attempt as a no-op rather than a success.
                        yield outcomeWriter.persist(record, trigger, decision.direction(), SyncOutcome.SKIPPED,
                                localHashForAttempt,
                                record.getExternal() != null ? record.getExternal().hash() : externalHashForAttempt,
                                null, actingUser);
                    }
                    resilientAdapterExecutor.call(() -> adapter.externalVersion(externalId)).ifPresent(record::setExternalVersion);
                    yield outcomeWriter.persist(record, trigger, decision.direction(), SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                }
            };
        } catch (RuntimeException failure) {
            return handleFailure(record, trigger, actingUser, failure);
        }
    }

    /**
     * Classifies a failed pass (design.md D10, D11) and records the outcome:
     * outage-shaped failures reschedule at the initial delay and count toward
     * nothing; retryable failures move the record to {@code RETRYING} with a
     * growing backoff, derived from the attempt history; anything else fails the
     * record on the spot.
     */
    private SyncRecord handleFailure(SyncRecord record, SyncTriggerKind trigger, String actingUser, RuntimeException failure) {
        FailureCategory category = FailureClassifier.classify(failure);
        String reason = failure.getMessage();
        Instant now = Instant.now();

        return switch (category) {
            case OUTAGE -> {
                // An outage failure reschedules at the initial delay, never the grown
                // one, and counts toward neither the failure count nor the backoff
                // (design.md D11).
                record.recordOutage(retryScheduler.nextAttemptDueAfterOutage(now));
                yield outcomeWriter.persist(record, trigger, null, SyncOutcome.OUTAGE, null, null, reason, actingUser);
            }
            case RETRYABLE -> {
                int failedAttempts = retryScheduler.failedAttemptsSince(syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(record.getId())) + 1;
                if (retryScheduler.hasReachedLimit(failedAttempts)) {
                    record.recordTerminalFailure(failedAttempts, reason);
                } else {
                    record.recordRetryableFailure(retryScheduler.nextAttemptDueAfter(failedAttempts, now));
                }
                yield outcomeWriter.persist(record, trigger, null, SyncOutcome.FAILED, null, null, reason, actingUser);
            }
            case TERMINAL -> {
                int failedAttempts = retryScheduler.failedAttemptsSince(syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(record.getId())) + 1;
                record.recordTerminalFailure(failedAttempts, reason);
                yield outcomeWriter.persist(record, trigger, null, SyncOutcome.FAILED, null, null, reason, actingUser);
            }
        };
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

        SyncSnapshot freshLocal = SyncSnapshot.of(resilientAdapterExecutor.call(() -> adapter.readLocal(entityId)), hasher);
        if (!freshLocal.matches(decisionLocal)) {
            return false;
        }

        resilientAdapterExecutor.run(() -> adapter.applyToLocal(entityId, currentExternal.projection()));

        SyncProjection postWriteLocal = resilientAdapterExecutor.call(() -> adapter.readLocal(entityId));
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

        resilientAdapterExecutor.run(() -> adapter.applyToExternal(externalId, currentLocal.projection()));

        SyncSnapshot freshLocal = SyncSnapshot.of(resilientAdapterExecutor.call(() -> adapter.readLocal(entityId)), hasher);
        if (!freshLocal.matches(currentLocal)) {
            record.recordOutwardWriteWithSkippedAdvance(currentLocal);
            return false;
        }

        record.recordSuccess(SyncDirection.OUTWARD, currentLocal, currentLocal);
        return true;
    }
}
