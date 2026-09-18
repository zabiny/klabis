package com.klabis.sync.application;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Clock;
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
    private final SyncScheduleRepository syncScheduleRepository;
    private final SynchronizationAdapterRegistry adapterRegistry;
    private final SyncProjectionHasher hasher;
    private final ResilientAdapterExecutor resilientAdapterExecutor;
    private final RetryScheduler retryScheduler;
    private final SyncRecordClaimer claimer;
    private final SyncOutcomeWriter outcomeWriter;
    private final SyncRecordCreator recordCreator;
    private final Clock clock;

    SynchronizationService(
            SyncRecordRepository syncRecordRepository,
            SyncAttemptRepository syncAttemptRepository,
            SyncScheduleRepository syncScheduleRepository,
            SynchronizationAdapterRegistry adapterRegistry,
            SyncProjectionHasher hasher,
            ResilientAdapterExecutor resilientAdapterExecutor,
            SyncProperties properties,
            SyncRecordClaimer claimer,
            SyncOutcomeWriter outcomeWriter,
            SyncRecordCreator recordCreator,
            Clock clock
    ) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncAttemptRepository = syncAttemptRepository;
        this.syncScheduleRepository = syncScheduleRepository;
        this.adapterRegistry = adapterRegistry;
        this.hasher = hasher;
        this.resilientAdapterExecutor = resilientAdapterExecutor;
        this.retryScheduler = new RetryScheduler(properties);
        this.claimer = claimer;
        this.outcomeWriter = outcomeWriter;
        this.recordCreator = recordCreator;
        this.clock = clock;
    }

    @Transactional
    @Override
    public SyncRecord enroll(SyncTarget target, ExternalReference externalReference) {
        adapterRegistry.find(target.entityType(), externalReference.system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(target.entityType(), externalReference.system()));

        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target, externalReference);
        SyncRecord saved = syncRecordRepository.save(record);
        // Same transaction as the record save (proposal.md task 4.7, task 2.6's "every
        // record has a schedule row" invariant).
        syncScheduleRepository.createFor(saved.getId());
        return saved;
    }

    /**
     * Brings in a record Klabis does not have (design.md "Domain Changes", D1-D4, D8):
     * three branches depending on what the external reference already resolves to.
     * Deliberately carries no {@code @Transactional} of its own — the external calls
     * in the "no pairing" branch and the pass that follows every branch must run with
     * no transaction open (design.md D8, D12); only the collaborators this method
     * calls (each a genuine cross-bean call, so their own {@code @Transactional}
     * boundaries actually apply) commit anything.
     */
    @Override
    public SyncRecord pullAndEnroll(SyncEntityType entityType, ExternalReference externalReference, String actingUser) {
        SynchronizationAdapter adapter = adapterRegistry.find(entityType, externalReference.system())
                .orElseThrow(() -> new UnknownSyncEntityTypeException(entityType, externalReference.system()));

        Optional<SyncRecord> existing = syncRecordRepository.findBySystemAndExternalId(externalReference.system(), externalReference.externalId());

        SyncRecordId recordId = existing
                .map(this::resolveExistingPairing)
                .orElseGet(() -> createAndPair(entityType, externalReference, adapter).getId());

        SyncRecord claimed = claimer.claim(recordId);
        return runPass(claimed, adapter, SyncTriggerKind.MANUAL, actingUser);
    }

    /**
     * Two of {@link #pullAndEnroll}'s three branches (design.md D5-D7): a retired
     * pairing is brought back into service; an active one awaiting a decision is
     * refused outright; any other active pairing is simply the one the pass that
     * follows will run against, letting the decision table resolve the direction —
     * "pull" does not mean the external side always wins (design.md D5).
     */
    private SyncRecordId resolveExistingPairing(SyncRecord record) {
        if (record.getStatus() == SyncStatus.RETIRED) {
            return reactivate(record).getId();
        }
        if (record.getStatus() == SyncStatus.CONFLICT || record.getStatus() == SyncStatus.FAILED) {
            throw new SyncRecordNeedsResolutionException(record.getId(), record.getStatus());
        }
        return record.getId();
    }

    /**
     * The "no pairing" branch (design.md D8's sequence diagram): reads the external
     * system with no transaction open, then creates the local entity and pairs it —
     * committing together (design.md D8) — through {@link SyncRecordCreator}, a
     * separate bean so that commit is a genuine cross-bean call.
     *
     * @throws UnsupportedOperationException if the adapter does not declare
     *                                        {@link SyncCapabilities#createsLocal()}
     *                                        (the default {@link SynchronizationAdapter#createLocal}
     *                                        throws it)
     */
    private SyncRecord createAndPair(SyncEntityType entityType, ExternalReference externalReference, SynchronizationAdapter adapter) {
        if (!adapter.capabilities().createsLocal()) {
            throw new UnsupportedOperationException(
                    "This adapter does not support creating the local side — declare SyncCapabilities.createsLocal and override createLocal");
        }
        SyncProjection externalProjection = resilientAdapterExecutor.call(() -> adapter.readExternal(externalReference.externalId()));
        String entityId = resilientAdapterExecutor.call(() -> adapter.createLocal(externalProjection));
        SyncTarget target = new SyncTarget(entityType, entityId);
        return recordCreator.createAndPair(target, externalReference);
    }

    /**
     * The "pairing retired" branch (design.md D6, D7): discards the stale baseline
     * and puts the pairing back into the pre-baseline state, committing on its own
     * before the pass that follows (design.md D8).
     */
    private SyncRecord reactivate(SyncRecord record) {
        ScheduleEffect scheduleEffect = record.reactivate(clock.instant());
        return recordCreator.persistReactivation(record, scheduleEffect);
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
            // Writes only through SyncScheduleRepository now (proposal.md task 4.3) —
            // no aggregate load beyond the id lookup above, no aggregate save, so this
            // can no longer race SyncOutcomeWriter#persist's optimistic lock on
            // sync_record at all; that race and its retry (design.md, "the race this
            // change removes") is exactly what moving scheduling off the aggregate was
            // for. sync_schedule has no version column (task 2.3) by design.
            Instant now = clock.instant();
            syncScheduleRepository.apply(record.getId(), ScheduleEffect.dirtySince(now));
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
        record.retire(clock.instant());
        syncRecordRepository.save(record);
    }

    @Transactional
    @Override
    public SyncRecord acknowledgeConflict(SyncRecordId id, String actingUser) {
        SyncRecord record = getOrThrow(id);
        requireConflict(record);

        Instant now = clock.instant();
        ConflictAcknowledgement acknowledgement = new ConflictAcknowledgement(
                record.getLocal().hash(), record.getExternal().hash(), now, actingUser);
        record.acknowledgeConflict(acknowledgement);

        SyncRecord saved = syncRecordRepository.save(record);
        syncAttemptRepository.save(SyncAttempt.record(saved.getId(), now, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS,
                saved.getLocal().hash(), saved.getExternal().hash(), null, actingUser));
        return saved;
    }

    /**
     * Resolves a standing, acknowledged conflict (design.md D6, D7). A resolution
     * never trusts stored snapshots: both sides are re-read through the adapter first,
     * with no transaction open (design.md D12), and the call proceeds only if the
     * fresh hash pair still equals the acknowledged one. If a side moved in between,
     * the record's refreshed snapshots and its schedule clear are saved together via
     * {@link SyncOutcomeWriter#persistConflictRefresh} — a genuine cross-bean call, so
     * its {@code @Transactional} boundary actually applies (this method itself carries
     * no {@code @Transactional} of its own) — the conflict is left standing (re-raised
     * so a subsequent GET shows the new collision), and the call is rejected.
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

        // One instant for the whole resolution: the record's timestamps and the
        // attempt row documenting it must not land on different instants.
        Instant now = clock.instant();

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
            // own even though the throw that follows ends the call in failure — the
            // schedule write joins it in the same transaction (design.md D15,
            // proposal.md task 4.2) via SyncOutcomeWriter#persistConflictRefresh — a
            // genuine cross-bean call, so @Transactional actually applies (see that
            // class's javadoc on why this must never be a self-invoked method here).
            ScheduleEffect refreshEffect = record.recordConflict(freshLocal, freshExternal, null, now);
            outcomeWriter.persistConflictRefresh(record, refreshEffect);
            throw new ConflictNotAcknowledgedException(id);
        }

        ScheduleEffect scheduleEffect;
        SyncRecord written = switch (resolution) {
            case INWARD -> {
                resilientAdapterExecutor.run(() -> adapter.applyToLocal(entityId, freshExternal.projection()));
                SyncSnapshot postWriteLocal = SyncSnapshot.of(resilientAdapterExecutor.call(() -> adapter.readLocal(entityId)), hasher);
                scheduleEffect = record.resolveWithDirection(SyncDirection.INWARD, postWriteLocal, freshExternal, now);
                yield record;
            }
            case OUTWARD -> {
                resilientAdapterExecutor.run(() -> adapter.applyToExternal(externalId, freshLocal.projection()));
                scheduleEffect = record.resolveWithDirection(SyncDirection.OUTWARD, freshLocal, freshLocal, now);
                yield record;
            }
            case ACCEPT_DIVERGENCE -> {
                scheduleEffect = record.acceptDivergence(freshLocal, freshExternal);
                yield record;
            }
        };

        return outcomeWriter.persistResolution(written, scheduleEffect, now, resolutionDirection(resolution), freshLocal.hash(), freshExternal.hash(), actingUser);
    }

    @Override
    public SyncRecord reset(SyncRecordId id, String actingUser) {
        SyncRecord record = getOrThrow(id);
        if (record.getStatus() != SyncStatus.FAILED) {
            throw new SyncRecordNotFailedException(id);
        }
        ScheduleEffect scheduleEffect = record.reset();
        return outcomeWriter.persist(record, scheduleEffect, clock.instant(), SyncTriggerKind.MANUAL, null, SyncOutcome.RESET, null, null, null, actingUser);
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

        // One instant for the whole pass: the record's timestamps and the attempt row
        // documenting this same pass must not land on different instants.
        Instant now = clock.instant();

        try {
            if (record.getBaseline() != null && record.getDirtySince() == null) {
                Optional<ExternalVersionToken> currentToken = resilientAdapterExecutor.call(() -> adapter.externalVersion(externalId));
                if (currentToken.isPresent() && currentToken.get().equals(record.getExternalVersion())) {
                    // Cheap change indicator unchanged and no local edit observed: skip
                    // the full read entirely (design.md D3). Still recorded as an
                    // attempt — D15 requires every attempt to appends a row. No
                    // scheduling method was called, so the schedule is left untouched.
                    return outcomeWriter.persist(record, ScheduleEffect.noChange(), now, trigger, null, SyncOutcome.SKIPPED,
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
                case NOTHING_TO_DO -> outcomeWriter.persist(record, ScheduleEffect.noChange(), now, trigger, null, SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                case CONVERGED -> {
                    // Both sides changed independently to the same value: rebase both
                    // baselines, write nothing (design.md D4).
                    ScheduleEffect scheduleEffect = record.recordConverged(currentLocal, now);
                    yield outcomeWriter.persist(record, scheduleEffect, now, trigger, null, SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                }
                case CONFLICT -> {
                    // Neither side is written while a conflict stands (design.md D6, D7).
                    ScheduleEffect scheduleEffect = record.recordConflict(currentLocal, currentExternal, decision.direction(), now);
                    yield outcomeWriter.persist(record, scheduleEffect, now, trigger, decision.direction(), SyncOutcome.CONFLICT, localHashForAttempt, externalHashForAttempt, null, actingUser);
                }
                case ADOPT_EXTERNAL, WRITE -> {
                    WriteOutcome writeOutcome = decision.direction() == SyncDirection.INWARD
                            ? writeInward(record, adapter, currentExternal, currentLocal, now)
                            : writeOutward(record, adapter, currentLocal, now);
                    if (!writeOutcome.written()) {
                        // The local side moved again between the decision read and the
                        // write (design.md D9): abort, leave the record due for the next
                        // pass, and record the attempt as a no-op rather than a success.
                        yield outcomeWriter.persist(record, writeOutcome.scheduleEffect(), now, trigger, decision.direction(), SyncOutcome.SKIPPED,
                                localHashForAttempt,
                                record.getExternal() != null ? record.getExternal().hash() : externalHashForAttempt,
                                null, actingUser);
                    }
                    resilientAdapterExecutor.call(() -> adapter.externalVersion(externalId)).ifPresent(record::setExternalVersion);
                    yield outcomeWriter.persist(record, writeOutcome.scheduleEffect(), now, trigger, decision.direction(), SyncOutcome.SUCCESS, localHashForAttempt, externalHashForAttempt, null, actingUser);
                }
            };
        } catch (RuntimeException failure) {
            return handleFailure(record, now, trigger, actingUser, failure);
        }
    }

    /**
     * Whether {@link #writeInward}/{@link #writeOutward} actually wrote, and the
     * {@link ScheduleEffect} the domain call it made returned — kept together so
     * {@link #runPass} cannot reach the {@code outcomeWriter.persist} call for either
     * branch without an effect in hand (proposal.md task 4.9).
     */
    private record WriteOutcome(boolean written, ScheduleEffect scheduleEffect) {
    }

    /**
     * Classifies a failed pass (design.md D10, D11) and records the outcome:
     * outage-shaped failures reschedule at the initial delay and count toward
     * nothing; retryable failures move the record to {@code RETRYING} with a
     * growing backoff, derived from the attempt history; anything else fails the
     * record on the spot.
     */
    private SyncRecord handleFailure(SyncRecord record, Instant now, SyncTriggerKind trigger, String actingUser, RuntimeException failure) {
        FailureCategory category = FailureClassifier.classify(failure);
        String reason = failure.getMessage();

        return switch (category) {
            case OUTAGE -> {
                // An outage failure reschedules at the initial delay, never the grown
                // one, and counts toward neither the failure count nor the backoff
                // (design.md D11).
                ScheduleEffect scheduleEffect = record.recordOutage(retryScheduler.nextAttemptDueAfterOutage(now));
                yield outcomeWriter.persist(record, scheduleEffect, now, trigger, null, SyncOutcome.OUTAGE, null, null, reason, actingUser);
            }
            case RETRYABLE -> {
                int failedAttempts = retryScheduler.failedAttemptsSince(syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(record.getId())) + 1;
                ScheduleEffect scheduleEffect = retryScheduler.hasReachedLimit(failedAttempts)
                        ? record.recordTerminalFailure(failedAttempts, reason, now)
                        : record.recordRetryableFailure(retryScheduler.nextAttemptDueAfter(failedAttempts, now));
                yield outcomeWriter.persist(record, scheduleEffect, now, trigger, null, SyncOutcome.FAILED, null, null, reason, actingUser);
            }
            case TERMINAL -> {
                int failedAttempts = retryScheduler.failedAttemptsSince(syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(record.getId())) + 1;
                ScheduleEffect scheduleEffect = record.recordTerminalFailure(failedAttempts, reason, now);
                yield outcomeWriter.persist(record, scheduleEffect, now, trigger, null, SyncOutcome.FAILED, null, null, reason, actingUser);
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
     * @return a {@link WriteOutcome} with {@code written = true} if the write happened,
     * {@code false} if aborted — and either way the {@link ScheduleEffect} the domain
     * call made produced
     */
    private WriteOutcome writeInward(SyncRecord record, SynchronizationAdapter adapter, SyncSnapshot currentExternal, SyncSnapshot decisionLocal, Instant now) {
        String entityId = record.getTarget().entityId();

        SyncSnapshot freshLocal = SyncSnapshot.of(resilientAdapterExecutor.call(() -> adapter.readLocal(entityId)), hasher);
        if (!freshLocal.matches(decisionLocal)) {
            return new WriteOutcome(false, ScheduleEffect.noChange());
        }

        resilientAdapterExecutor.run(() -> adapter.applyToLocal(entityId, currentExternal.projection()));

        SyncProjection postWriteLocal = resilientAdapterExecutor.call(() -> adapter.readLocal(entityId));
        SyncSnapshot postWriteSnapshot = SyncSnapshot.of(postWriteLocal, hasher);

        ScheduleEffect scheduleEffect = record.recordSuccess(SyncDirection.INWARD, postWriteSnapshot, currentExternal, now);
        return new WriteOutcome(true, scheduleEffect);
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
     * @return a {@link WriteOutcome} with {@code written = true} if the baseline was
     * written, {@code false} if skipped — and either way the {@link ScheduleEffect}
     * the domain call made produced
     */
    private WriteOutcome writeOutward(SyncRecord record, SynchronizationAdapter adapter, SyncSnapshot currentLocal, Instant now) {
        String entityId = record.getTarget().entityId();
        String externalId = record.getExternalReference().externalId();

        resilientAdapterExecutor.run(() -> adapter.applyToExternal(externalId, currentLocal.projection()));

        SyncSnapshot freshLocal = SyncSnapshot.of(resilientAdapterExecutor.call(() -> adapter.readLocal(entityId)), hasher);
        if (!freshLocal.matches(currentLocal)) {
            ScheduleEffect scheduleEffect = record.recordOutwardWriteWithSkippedAdvance(currentLocal, now);
            return new WriteOutcome(false, scheduleEffect);
        }

        ScheduleEffect scheduleEffect = record.recordSuccess(SyncDirection.OUTWARD, currentLocal, currentLocal, now);
        return new WriteOutcome(true, scheduleEffect);
    }
}
