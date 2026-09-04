package com.klabis.sync.application;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists the outcome of one pass — the record and its attempt row — atomically
 * (design.md D15: every attempt must appear in the history, so a crash between two
 * separately-committed writes must never happen).
 * <p>
 * A separate bean from {@link SynchronizationService}, not a private/protected method
 * on it: {@code SynchronizationService} calls these methods after phase 2 (the
 * external call, with no transaction open — design.md D12), and only a genuine
 * cross-bean call goes through the Spring AOP proxy that applies
 * {@code @Transactional} — a self-invoked method on the same bean would silently skip
 * the transaction boundary.
 */
@Service
class SyncOutcomeWriter {

    private final SyncRecordRepository syncRecordRepository;
    private final SyncAttemptRepository syncAttemptRepository;
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
                       @Lazy SyncOutcomeWriter self) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncAttemptRepository = syncAttemptRepository;
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
                () -> self.doPersist(record, trigger, direction, outcome, localHash, externalHash, failureReason, actingUser));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SyncRecord doPersist(
            SyncRecord record,
            SyncTriggerKind trigger,
            SyncDirection direction,
            SyncOutcome outcome,
            SyncHash localHash,
            SyncHash externalHash,
            String failureReason,
            String actingUser
    ) {
        SyncRecord saved = syncRecordRepository.save(record);
        appendAttempt(saved, trigger, direction, outcome, localHash, externalHash, failureReason, actingUser);
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
    SyncRecord persistResolution(SyncRecord record, SyncDirection direction, SyncHash localHash, SyncHash externalHash, String actingUser) {
        return withOptimisticLockRetry(record,
                () -> self.doPersistResolution(record, direction, localHash, externalHash, actingUser));
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
    SyncRecord doPersistResolution(SyncRecord record, SyncDirection direction, SyncHash localHash, SyncHash externalHash, String actingUser) {
        SyncRecord saved = syncRecordRepository.save(record);
        appendAttempt(saved, SyncTriggerKind.MANUAL, direction, SyncOutcome.SUCCESS, localHash, externalHash, null, actingUser);
        return saved;
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
