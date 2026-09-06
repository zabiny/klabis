package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ScheduleEffect;
import com.klabis.sync.domain.SyncSchedule;
import com.klabis.sync.domain.SyncScheduleRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

/**
 * {@code sync_schedule} has no version column by design (proposal.md task 2.3), so
 * {@link #apply} is a plain read-modify-write with no optimistic-lock retry to worry
 * about — the whole point of splitting scheduling out of {@code sync_record} is that
 * a concurrent {@code markDirty} write and an outcome-writer save no longer contend
 * for the same lock at all.
 */
@SecondaryAdapter
@Repository
class SyncScheduleRepositoryAdapter implements SyncScheduleRepository {

    private final SyncScheduleJdbcRepository jdbcRepository;

    SyncScheduleRepositoryAdapter(SyncScheduleJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public SyncSchedule findByRecordId(SyncRecordId recordId) {
        return jdbcRepository.findById(recordId.value())
                .map(SyncScheduleMemento::toSyncSchedule)
                // Coded default (proposal.md task 2.6) for a record enrolled before this
                // change, or any other path that reaches here without a schedule row —
                // no production data exists yet, so there is nothing to backfill.
                .orElseGet(SyncSchedule::empty);
    }

    @Override
    public void apply(SyncRecordId recordId, ScheduleEffect effect) {
        var existingRow = jdbcRepository.findById(recordId.value());
        SyncSchedule current = existingRow.map(SyncScheduleMemento::toSyncSchedule).orElseGet(SyncSchedule::empty);
        SyncSchedule next = current.apply(effect);
        jdbcRepository.save(SyncScheduleMemento.existing(recordId.value(), next, existingRow.isPresent()));
    }

    @Override
    public void createFor(SyncRecordId recordId) {
        jdbcRepository.save(SyncScheduleMemento.newFor(recordId.value()));
    }
}
