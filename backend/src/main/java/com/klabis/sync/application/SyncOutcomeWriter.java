package com.klabis.sync.application;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Persists the outcome of one pass — the record, its schedule and its attempt row —
 * atomically (design.md D15: every attempt must appear in the history, so a crash
 * between two separately-committed writes must never happen).
 * <p>
 * {@code scheduleEffect} is a required parameter on {@link #persist} and
 * {@link #persistResolution} (proposal.md task 4.9): a caller cannot reach either
 * method without a {@link ScheduleEffect} in hand, which is the structural guard
 * against the dropped-effect trap the return-type-only signature would otherwise
 * allow — silently discarding the {@code ScheduleEffect} a domain method returns
 * compiles cleanly but loses the scheduling write with no failing test.
 * {@link ScheduleEffect#noChange()} is the explicit value for a pass that never
 * called a scheduling method (e.g. {@code NOTHING_TO_DO}, a skipped write).
 * <p>
 * A separate bean from {@link SynchronizationService}, not a private/protected method
 * on it: {@code SynchronizationService} calls these methods after phase 2 (the
 * external call, with no transaction open — design.md D12), and only a genuine
 * cross-bean call goes through the Spring AOP proxy that applies
 * {@code @Transactional} — a self-invoked method on the same bean would silently skip
 * the transaction boundary.
 * <p>
 * The instant that stamps the attempt row is passed in by the caller, not read here:
 * {@link SynchronizationService} captures {@code clock.instant()} once per logical
 * operation and threads the same value into the record's own timestamps and this
 * attempt row, so the two never drift onto different instants.
 */
@Service
class SyncOutcomeWriter {

    private final SyncRecordRepository syncRecordRepository;
    private final SyncAttemptRepository syncAttemptRepository;
    private final SyncScheduleRepository syncScheduleRepository;
    private final SyncOutcomeWriter self;

    /**
     * {@code self} is this bean's own Spring proxy, injected lazily to sidestep the
     * construction cycle a self-reference would otherwise create. {@link #persist} is
     * deliberately not {@code @Transactional} itself — the version-conflict retry needs
     * its two attempts to run as two separate transactions, and only a call through the
     * proxy (never a plain {@code this.doPersist(...)}) makes {@code @Transactional}
     * apply at all.
     */
    SyncOutcomeWriter(SyncRecordRepository syncRecordRepository, SyncAttemptRepository syncAttemptRepository,
                       SyncScheduleRepository syncScheduleRepository, @Lazy SyncOutcomeWriter self) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncAttemptRepository = syncAttemptRepository;
        this.syncScheduleRepository = syncScheduleRepository;
        this.self = self;
    }

    /**
     * Persists the record and appends an attempt row (design.md D15), releasing the
     * record's claim in the same transaction — a persisted outcome, of whatever kind,
     * is the natural end of that record's claim window (design.md D12).
     * <p>
     * An inward write raises {@code EventUpdatedEvent} on the very entity the pass just
     * wrote (design.md D9 — "an inward write is itself a local change"), and the
     * self-listener's {@code markDirty} call runs asynchronously against the same
     * {@code sync_record} row this method is about to save — so this save can lose an
     * optimistic-locking race it did nothing wrong to lose. One retry against the
     * current stored version is safe: {@code record}'s pass-computed state (status,
     * snapshots, attempt) is unaffected by a concurrent dirty-marker, only the version
     * stamp is stale. The retry runs in a brand-new transaction ({@code REQUIRES_NEW})
     * rather than inside the failed one — Spring marks a transaction rollback-only the
     * moment an exception crosses its boundary, so retrying within it would only trade
     * {@link OptimisticLockingFailureException} for
     * {@link org.springframework.transaction.UnexpectedRollbackException} on commit.
     */
    SyncRecord persist(
            SyncRecord record,
            ScheduleEffect scheduleEffect,
            Instant startedAt,
            SyncTriggerKind trigger,
            SyncDirection direction,
            SyncOutcome outcome,
            SyncHash localHash,
            SyncHash externalHash,
            String failureReason,
            String actingUser
    ) {
        record.releaseClaim();
        return withOptimisticLockRetry(record,
                () -> self.doPersist(record, scheduleEffect, startedAt, trigger, direction, outcome, localHash, externalHash, failureReason, actingUser));
    }

    /**
     * The schedule write happens in this same {@code REQUIRES_NEW} transaction as the
     * record save and attempt append (design.md D15, proposal.md task 4.2) — a schedule
     * write committed outside it could survive a rolled-back outcome. {@code
     * sync_schedule} has no version column (task 2.3), so this is a plain
     * read-modify-write with nothing to retry if {@code doPersist} itself is retried by
     * {@link #withOptimisticLockRetry} — reapplying the same effect twice is idempotent
     * (each {@link ScheduleEffect} sets an absolute value, never increments one).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SyncRecord doPersist(
            SyncRecord record,
            ScheduleEffect scheduleEffect,
            Instant startedAt,
            SyncTriggerKind trigger,
            SyncDirection direction,
            SyncOutcome outcome,
            SyncHash localHash,
            SyncHash externalHash,
            String failureReason,
            String actingUser
    ) {
        SyncRecord saved = syncRecordRepository.save(record);
        syncScheduleRepository.apply(saved.getId(), scheduleEffect);
        appendAttempt(saved, startedAt, trigger, direction, outcome, localHash, externalHash, failureReason, actingUser);
        return saved;
    }

    /**
     * Persists a resolved conflict's record and attempt together (design.md D15). No
     * claim to release — conflict resolution does not go through the claim mechanism
     * (D12's claim guards scheduled/manual pass overlap; a resolution is always an
     * explicit, single manager action against an already-standing conflict).
     * <p>
     * Subject to the same {@code markDirty} race {@link #persist} guards against — an
     * {@code INWARD} resolution writes the local side just as an ordinary inward pass
     * does — so it gets the same version-conflict retry in a fresh transaction.
     */
    SyncRecord persistResolution(SyncRecord record, ScheduleEffect scheduleEffect, Instant startedAt, SyncDirection direction, SyncHash localHash, SyncHash externalHash, String actingUser) {
        return withOptimisticLockRetry(record,
                () -> self.doPersistResolution(record, scheduleEffect, startedAt, direction, localHash, externalHash, actingUser));
    }

    /**
     * Runs {@code write} once, and — on the {@code markDirty} race both {@link
     * #persist} and {@link #persistResolution} are exposed to (see their own
     * javadoc) — refreshes {@code record}'s version stamp from the currently stored
     * row and retries exactly once. The retried call still goes through the {@code
     * self} proxy inside {@code write}, so it still runs in its own fresh {@code
     * REQUIRES_NEW} transaction.
     */
    private SyncRecord withOptimisticLockRetry(SyncRecord record, java.util.function.Supplier<SyncRecord> write) {
        try {
            return write.get();
        } catch (OptimisticLockingFailureException raced) {
            Long currentVersion = syncRecordRepository.findById(record.getId())
                    .map(SyncRecord::getVersion)
                    .orElseThrow(() -> raced);
            record.updateAuditMetadata(new AuditMetadata(
                    record.getCreatedAt(), record.getCreatedBy(),
                    record.getLastModifiedAt(), record.getLastModifiedBy(),
                    currentVersion));
            return write.get();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SyncRecord doPersistResolution(SyncRecord record, ScheduleEffect scheduleEffect, Instant startedAt, SyncDirection direction, SyncHash localHash, SyncHash externalHash, String actingUser) {
        SyncRecord saved = syncRecordRepository.save(record);
        syncScheduleRepository.apply(saved.getId(), scheduleEffect);
        appendAttempt(saved, startedAt, SyncTriggerKind.MANUAL, direction, SyncOutcome.SUCCESS, localHash, externalHash, null, actingUser);
        return saved;
    }

    private void appendAttempt(
            SyncRecord record,
            Instant startedAt,
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
                record.getId(), startedAt, trigger, direction, outcome, localHash, externalHash, failureReason, recordedActingUser));
    }
}
