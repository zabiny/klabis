package com.klabis.sync.domain;

import com.klabis.sync.SyncAttemptId;
import com.klabis.sync.SyncRecordId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * One recorded synchronisation attempt: the audit trail (design.md D15). Separate
 * from {@link SyncRecord} because the history is unbounded and must not be loaded
 * with the record — it is appended to, queried to derive the failure count (D10), and
 * pruned by retention (D19), never loaded as part of a {@code SyncRecord}.
 * <p>
 * Immutable once created — an attempt is a fact about what happened, not a piece of
 * mutable state.
 */
@AggregateRoot
public final class SyncAttempt {

    @Identity
    private final SyncAttemptId id;
    private final SyncRecordId recordId;
    private final Instant startedAt;
    private final SyncTriggerKind trigger;
    private final SyncDirection direction;
    private final SyncOutcome outcome;
    private final SyncHash localHash;
    private final SyncHash externalHash;
    private final String failureReason;
    private final String actingUser;

    private SyncAttempt(
            SyncAttemptId id,
            SyncRecordId recordId,
            Instant startedAt,
            SyncTriggerKind trigger,
            SyncDirection direction,
            SyncOutcome outcome,
            SyncHash localHash,
            SyncHash externalHash,
            String failureReason,
            String actingUser
    ) {
        Assert.notNull(id, "id is required");
        Assert.notNull(recordId, "recordId is required");
        Assert.notNull(startedAt, "startedAt is required");
        Assert.notNull(trigger, "trigger is required");
        Assert.notNull(outcome, "outcome is required");
        this.id = id;
        this.recordId = recordId;
        this.startedAt = startedAt;
        this.trigger = trigger;
        this.direction = direction;
        this.outcome = outcome;
        this.localHash = localHash;
        this.externalHash = externalHash;
        this.failureReason = failureReason;
        this.actingUser = actingUser;
    }

    public static SyncAttempt record(
            SyncRecordId recordId,
            SyncTriggerKind trigger,
            SyncDirection direction,
            SyncOutcome outcome,
            SyncHash localHash,
            SyncHash externalHash,
            String failureReason,
            String actingUser
    ) {
        return new SyncAttempt(
                SyncAttemptId.newId(),
                recordId,
                Instant.now(),
                trigger,
                direction,
                outcome,
                localHash,
                externalHash,
                failureReason,
                actingUser
        );
    }

    public static SyncAttempt reconstruct(
            SyncAttemptId id,
            SyncRecordId recordId,
            Instant startedAt,
            SyncTriggerKind trigger,
            SyncDirection direction,
            SyncOutcome outcome,
            SyncHash localHash,
            SyncHash externalHash,
            String failureReason,
            String actingUser
    ) {
        return new SyncAttempt(id, recordId, startedAt, trigger, direction, outcome, localHash, externalHash, failureReason, actingUser);
    }

    public SyncAttemptId getId() {
        return id;
    }

    public SyncRecordId getRecordId() {
        return recordId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public SyncTriggerKind getTrigger() {
        return trigger;
    }

    public SyncDirection getDirection() {
        return direction;
    }

    public SyncOutcome getOutcome() {
        return outcome;
    }

    public SyncHash getLocalHash() {
        return localHash;
    }

    public SyncHash getExternalHash() {
        return externalHash;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getActingUser() {
        return actingUser;
    }
}
