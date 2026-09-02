package com.klabis.sync.application;

import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Service;
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

    SyncOutcomeWriter(SyncRecordRepository syncRecordRepository, SyncAttemptRepository syncAttemptRepository) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncAttemptRepository = syncAttemptRepository;
    }

    /**
     * Persists the record and appends an attempt row (design.md D15), releasing the
     * record's claim in the same transaction — a persisted outcome, of whatever kind,
     * is the natural end of that record's claim window (design.md D12).
     */
    @Transactional
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
        SyncRecord saved = syncRecordRepository.save(record);
        appendAttempt(saved, trigger, direction, outcome, localHash, externalHash, failureReason, actingUser);
        return saved;
    }

    /**
     * Persists a resolved conflict's record and attempt together (design.md D15). No
     * claim to release — conflict resolution does not go through the claim mechanism
     * (D12's claim guards scheduled/manual pass overlap; a resolution is always an
     * explicit, single manager action against an already-standing conflict).
     */
    @Transactional
    SyncRecord persistResolution(SyncRecord record, SyncDirection direction, SyncHash localHash, SyncHash externalHash, String actingUser) {
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
