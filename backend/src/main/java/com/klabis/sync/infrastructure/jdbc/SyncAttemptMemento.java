package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.SyncAttemptId;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

/**
 * Memento for {@link SyncAttempt} — append-only, holds hashes only, never projections
 * (design.md D15).
 */
@Table(schema = "sync", value = "sync_attempt")
class SyncAttemptMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("sync_record_id")
    private UUID syncRecordId;

    @Column("started_at")
    private Instant startedAt;

    @Column("trigger")
    private String trigger;

    @Column("direction")
    private String direction;

    @Column("outcome")
    private String outcome;

    @Column("local_hash")
    private String localHash;

    @Column("external_hash")
    private String externalHash;

    @Column("failure_reason")
    private String failureReason;

    @Column("acting_user")
    private String actingUser;

    @Transient
    private final boolean isNew;

    protected SyncAttemptMemento() {
        this.isNew = true;
    }

    private SyncAttemptMemento(boolean isNew) {
        this.isNew = isNew;
    }

    static SyncAttemptMemento from(SyncAttempt attempt) {
        Assert.notNull(attempt, "attempt must not be null");

        SyncAttemptMemento memento = new SyncAttemptMemento(true);
        memento.id = attempt.getId().value();
        memento.syncRecordId = attempt.getRecordId().value();
        memento.startedAt = attempt.getStartedAt();
        memento.trigger = attempt.getTrigger().name();
        memento.direction = attempt.getDirection() != null ? attempt.getDirection().name() : null;
        memento.outcome = attempt.getOutcome().name();
        memento.localHash = attempt.getLocalHash() != null ? attempt.getLocalHash().value() : null;
        memento.externalHash = attempt.getExternalHash() != null ? attempt.getExternalHash().value() : null;
        memento.failureReason = attempt.getFailureReason();
        memento.actingUser = attempt.getActingUser();
        return memento;
    }

    SyncAttempt toSyncAttempt() {
        return SyncAttempt.reconstruct(
                new SyncAttemptId(id),
                new SyncRecordId(syncRecordId),
                startedAt,
                SyncTriggerKind.valueOf(trigger),
                direction != null ? SyncDirection.valueOf(direction) : null,
                SyncOutcome.valueOf(outcome),
                localHash != null ? SyncHash.of(localHash) : null,
                externalHash != null ? SyncHash.of(externalHash) : null,
                failureReason,
                actingUser
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
