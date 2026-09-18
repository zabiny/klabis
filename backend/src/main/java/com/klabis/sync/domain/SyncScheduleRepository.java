package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Persists a record's {@link SyncSchedule} — deliberately its own port, separate from
 * {@link SyncRecordRepository}, since a scheduling write must never take the same
 * optimistic lock as the aggregate's own version (proposal.md "Scheduling moves out of
 * the aggregate into its own table and its own port").
 */
@SecondaryPort
public interface SyncScheduleRepository {

    /**
     * Loads the schedule for a record, or {@link SyncSchedule#empty()} if none exists
     * yet — every record gets a schedule row at enrolment (proposal.md task 2.6), so a
     * missing row is not expected in production, but a read path should never have to
     * special-case a missing one.
     */
    SyncSchedule findByRecordId(SyncRecordId recordId);

    /**
     * Applies a {@link ScheduleEffect} to the record's schedule and persists the
     * result. Called from the same transaction as the record save and attempt append
     * (design.md D15) — a schedule write outside that transaction could survive a
     * rolled-back outcome.
     */
    void apply(SyncRecordId recordId, ScheduleEffect effect);

    /**
     * Creates the schedule row for a newly enrolled record (proposal.md task 2.6): a
     * record always has a schedule from enrolment onward, so no later read path has to
     * handle a missing one.
     */
    void createFor(SyncRecordId recordId);
}
