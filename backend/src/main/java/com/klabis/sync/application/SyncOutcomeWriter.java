package com.klabis.sync.application;

import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Service;
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
 * {@code @Transactional} at all — a self-invoked method on the same bean would
 * silently skip the transaction boundary. That is still the reason this stays a
 * separate bean even though the retry that used to also depend on the proxy boundary
 * is gone (proposal.md "Atomicity is preserved").
 * <p>
 * {@code sync_record}'s version no longer contends with {@code markDirty}: scheduling
 * ({@code dirtySince}/{@code nextAttemptDueAt}) moved to the unversioned
 * {@code sync_schedule} table, so {@code markDirty} writes through
 * {@code SyncScheduleRepository} alone and never loads or saves this aggregate
 * (proposal.md "Why"). {@link #persist} and {@link #persistResolution} are therefore
 * plain, single-attempt {@code @Transactional} methods — no version-conflict retry, no
 * fresh-transaction propagation to give a retry somewhere to run, and no self-proxy to
 * make that retry transactional.
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

    SyncOutcomeWriter(SyncRecordRepository syncRecordRepository, SyncAttemptRepository syncAttemptRepository,
                       SyncScheduleRepository syncScheduleRepository) {
        this.syncRecordRepository = syncRecordRepository;
        this.syncAttemptRepository = syncAttemptRepository;
        this.syncScheduleRepository = syncScheduleRepository;
    }

    /**
     * Persists the record and appends an attempt row (design.md D15), releasing the
     * record's claim in the same transaction — a persisted outcome, of whatever kind,
     * is the natural end of that record's claim window (design.md D12).
     * <p>
     * The schedule write happens in the same transaction as the record save and
     * attempt append (design.md D15, proposal.md task 4.2) — a schedule write
     * committed outside it could survive a rolled-back outcome.
     */
    @Transactional
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
        SyncRecord saved = syncRecordRepository.save(record);
        syncScheduleRepository.apply(saved.getId(), scheduleEffect);
        // saved is a separate object built by the record repository before the
        // schedule effect above was applied — without this its dirtySince/
        // nextAttemptDueAt would be stale relative to what was just persisted. record's
        // own in-memory schedule is already correct (the domain method that produced
        // scheduleEffect mutated it), so copy that rather than re-querying the schedule
        // repository.
        saved.updateSchedule(new SyncSchedule(record.getDirtySince(), record.getNextAttemptDueAt()));
        appendAttempt(saved, startedAt, trigger, direction, outcome, localHash, externalHash, failureReason, actingUser);
        return saved;
    }

    /**
     * Persists a resolved conflict's record and attempt together (design.md D15). No
     * claim to release — conflict resolution does not go through the claim mechanism
     * (D12's claim guards scheduled/manual pass overlap; a resolution is always an
     * explicit, single manager action against an already-standing conflict).
     */
    @Transactional
    SyncRecord persistResolution(SyncRecord record, ScheduleEffect scheduleEffect, Instant startedAt, SyncDirection direction, SyncHash localHash, SyncHash externalHash, String actingUser) {
        SyncRecord saved = syncRecordRepository.save(record);
        syncScheduleRepository.apply(saved.getId(), scheduleEffect);
        // See persist's comment: saved's schedule predates the apply() call above.
        saved.updateSchedule(new SyncSchedule(record.getDirtySince(), record.getNextAttemptDueAt()));
        appendAttempt(saved, startedAt, SyncTriggerKind.MANUAL, direction, SyncOutcome.SUCCESS, localHash, externalHash, null, actingUser);
        return saved;
    }

    /**
     * Persists a conflict's refreshed snapshots and its {@code recordConflict}
     * schedule effect together (design.md D15, proposal.md task 4.2) — used by
     * {@link SynchronizationService#resolveConflict} when a side moved since the
     * acknowledgement being checked, so the refresh must land atomically even though
     * the call ultimately still throws {@code ConflictNotAcknowledgedException} back
     * to the caller.
     */
    @Transactional
    void persistConflictRefresh(SyncRecord record, ScheduleEffect scheduleEffect) {
        syncRecordRepository.save(record);
        syncScheduleRepository.apply(record.getId(), scheduleEffect);
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
