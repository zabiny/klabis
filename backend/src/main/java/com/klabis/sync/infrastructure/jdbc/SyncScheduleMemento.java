package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.domain.SyncSchedule;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Memento for {@link SyncSchedule}, keyed by the owning record's id — no surrogate id
 * and, deliberately, no version column (proposal.md task 2.3): a scheduling write must
 * never contend for the same optimistic lock as {@code sync_record}. Only used to read
 * a schedule and to insert its row at enrolment — every other write goes through
 * {@link SyncScheduleJdbcRepository}'s unconditional {@code UPDATE} queries, not
 * {@code CrudRepository.save}, so this memento never has to distinguish an update from
 * an insert for those paths.
 */
@Table(schema = "sync", value = "sync_schedule")
class SyncScheduleMemento implements Persistable<UUID> {

    @Id
    @Column("sync_record_id")
    private UUID syncRecordId;

    @Column("dirty_since")
    private Instant dirtySince;

    @Column("next_attempt_due_at")
    private Instant nextAttemptDueAt;

    @Transient
    private boolean isNew;

    protected SyncScheduleMemento() {
    }

    static SyncScheduleMemento newFor(UUID syncRecordId) {
        SyncScheduleMemento memento = new SyncScheduleMemento();
        memento.syncRecordId = syncRecordId;
        memento.isNew = true;
        return memento;
    }

    SyncSchedule toSyncSchedule() {
        return new SyncSchedule(dirtySince, nextAttemptDueAt);
    }

    @Override
    public UUID getId() {
        return syncRecordId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
