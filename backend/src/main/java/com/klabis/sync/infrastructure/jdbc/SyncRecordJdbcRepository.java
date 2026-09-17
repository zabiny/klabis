package com.klabis.sync.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface SyncRecordJdbcRepository extends CrudRepository<SyncRecordMemento, UUID> {

    Optional<SyncRecordMemento> findByEntityTypeAndEntityIdAndExternalSystem(String entityType, String entityId, String externalSystem);

    /**
     * Looks a pairing up from the external side, every status included — {@code
     * RETIRED} deliberately so (see
     * {@link com.klabis.sync.domain.SyncRecordRepository#findBySystemAndExternalId}).
     * Rides {@code uq_sync_record_external UNIQUE (external_system, external_id,
     * entity_type)} (V001__initial_schema.sql): that constraint's own index serves
     * this two-column prefix of its columns, so no new index is needed.
     */
    Optional<SyncRecordMemento> findByExternalSystemAndExternalId(String externalSystem, String externalId);

    /**
     * Backs the nightly full pass (design.md D10, D17): every record still eligible
     * to be attempted by the scheduler. Excludes {@code RETIRED} (D17, no longer
     * scanned) and {@code FAILED} (D10, "skipped by the scheduler" until a manager
     * resets it) — {@code SynchronizationService.runScheduledPass} asserts it is never
     * called for either status, so this query is what keeps that assertion from ever
     * tripping in production. {@code CONFLICT} stays included: D7 has a standing
     * conflict recomputed on every pass, which the full pass is the one to do.
     */
    @Query("""
            SELECT * FROM sync.sync_record
            WHERE retired_at IS NULL
              AND status <> 'FAILED'
            """)
    List<SyncRecordMemento> findAllActive();

    /**
     * Backs the manual {@code all-upcoming} bulk pass (design.md, "Existing operations,
     * unchanged in shape"): every non-retired record, {@code FAILED} included, so a
     * terminally failed record can be counted and reported rather than silently
     * dropped. Unlike {@link #findAllActive}, which the scheduler needs
     * {@code FAILED}-free.
     */
    @Query("""
            SELECT * FROM sync.sync_record
            WHERE retired_at IS NULL
            """)
    List<SyncRecordMemento> findAllNonRetired();

    /**
     * Backs the due scan (design.md D10): dirty or retry-due, excluding a record
     * whose claim is still fresh. {@code RETIRED} is excluded via
     * {@code retired_at IS NULL}. Neither {@code FAILED} nor {@code CONFLICT} is
     * excluded by construction: {@code recordConflict} and {@code recordTerminalFailure}
     * do clear {@code sync_schedule}'s columns via the {@code ScheduleEffect} they
     * return, but {@code SyncScheduleRepository.apply} has no status guard, so an
     * ordinary local edit to an entity whose record is already {@code CONFLICT} or
     * {@code FAILED} re-sets {@code sync_schedule.dirty_since} and the record then
     * matches the dirty predicate. Handed to a pass in that state it is rejected by
     * {@code SyncRecord.assertBeingAttempted} once {@code runScheduledPass} tries it, an
     * exception the scheduler's per-record handler catches and logs at ERROR. The
     * {@code status NOT IN ('FAILED', 'CONFLICT')} predicate keeps that exclusion local
     * to the query; {@code FAILED} matches what {@link #findAllActive} does for the
     * nightly full pass (see
     * {@link com.klabis.sync.domain.SyncRecordRepository#findDueForScan}). A standing
     * conflict still waits for a manager to resolve it — a resolution re-enrolls the
     * record and clears {@code CONFLICT}, at which point it is eligible again. One
     * indexed query, matching the index on {@code (dirty_since, next_attempt_due_at)}
     * declared on {@code sync_schedule} (proposal.md task 2.4).
     * <p>
     * {@code LEFT JOIN}, not {@code INNER JOIN} (proposal.md task 4b.1): every record
     * gets a {@code sync_schedule} row at enrolment (task 2.6, 4.7), but this query
     * must not depend on that invariant holding for every row that predates it — an
     * {@code INNER JOIN} would silently drop a record with no schedule row from the
     * scan (it would simply never be synchronised again, with no error anywhere). A
     * missing row behaves exactly as {@link com.klabis.sync.domain.SyncSchedule#empty()}
     * does everywhere else in this change: {@code dirty_since IS NULL} (never dirty)
     * and {@code next_attempt_due_at <= :now} is false when the column itself is
     * {@code NULL}, so such a record is due only once something else marks it dirty or
     * schedules a retry — the same behaviour a freshly enrolled record with an empty
     * schedule has today.
     */
    @Query("""
            SELECT sr.* FROM sync.sync_record sr
            LEFT JOIN sync.sync_schedule ss ON ss.sync_record_id = sr.id
            WHERE sr.retired_at IS NULL
              AND sr.status NOT IN ('FAILED', 'CONFLICT')
              AND (ss.dirty_since IS NOT NULL OR ss.next_attempt_due_at <= :now)
              AND (sr.claimed_at IS NULL OR sr.claimed_at <= :claimStaleBefore)
            """)
    List<SyncRecordMemento> findDueForScan(@Param("now") Instant now, @Param("claimStaleBefore") Instant claimStaleBefore);
}
