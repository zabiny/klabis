package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ScheduleEffect;
import com.klabis.sync.domain.SyncSchedule;
import com.klabis.sync.domain.SyncScheduleRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.UUID;

/**
 * {@code sync_schedule} has no version column by design (proposal.md task 2.3): a
 * scheduling write must never contend for the aggregate's optimistic lock. That does
 * not make it safe to read-modify-write in Java, though — two effects that touch
 * different columns (e.g. {@code markDirty}'s {@code DIRTY_SINCE} racing
 * {@code SyncOutcomeWriter}'s {@code DUE_AT}) are not independent of each other's
 * *column*, and a read-modify-write over the whole row lets the second writer's
 * fetch-before-the-first-writer's-save silently discard the first writer's field
 * (found by the simplify review: a lost {@code dirtySince} delays the affected record's
 * next sync by up to the retry backoff, with no error). {@link #apply} instead issues
 * one unconditional, single-column {@code UPDATE} per {@link ScheduleEffect} kind — the
 * database row lock taken by that {@code UPDATE} is the only synchronisation needed,
 * and two concurrent effects on different columns both survive.
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
        UUID id = recordId.value();
        // NO_CHANGE is the effect most passes actually return (NOTHING_TO_DO and
        // SKIPPED outcomes — SynchronizationService.java:351, :369, :465) — skip the
        // round-trip entirely rather than issue a no-op UPDATE.
        switch (effect.kind()) {
            case NO_CHANGE -> {
            }
            case CLEAR -> jdbcRepository.updateClear(id);
            case CLEAR_DUE_AT -> jdbcRepository.updateClearDueAt(id);
            case DUE_AT -> {
                if (jdbcRepository.updateDueAt(id, effect.value()) == 0) {
                    jdbcRepository.insertIfMissingDueAt(id, effect.value());
                }
            }
            case DIRTY_SINCE -> {
                if (jdbcRepository.updateDirtySince(id, effect.value()) == 0) {
                    jdbcRepository.insertIfMissingDirtySince(id, effect.value());
                }
            }
        }
    }

    @Override
    public void createFor(SyncRecordId recordId) {
        jdbcRepository.save(SyncScheduleMemento.newFor(recordId.value()));
    }
}
