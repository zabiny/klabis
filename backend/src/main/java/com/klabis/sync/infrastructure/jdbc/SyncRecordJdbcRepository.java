package com.klabis.sync.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
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
     * do clear the scheduling fields, but {@code SyncRecord.markDirty} has no status
     * guard, so an ordinary local edit to an entity whose record is already
     * {@code CONFLICT} or {@code FAILED} re-sets {@code dirty_since} and the record then
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
     * declared in the schema.
     */
    @Query("""
            SELECT * FROM sync.sync_record
            WHERE retired_at IS NULL
              AND status NOT IN ('FAILED', 'CONFLICT')
              AND (dirty_since IS NOT NULL OR next_attempt_due_at <= :now)
              AND (claimed_at IS NULL OR claimed_at <= :claimStaleBefore)
            """)
    List<SyncRecordMemento> findDueForScan(@Param("now") Instant now, @Param("claimStaleBefore") Instant claimStaleBefore);

    /**
     * Transitional double-write (proposal.md task 4.10): a plain column update with
     * no {@code version} predicate, so it never contends for {@code sync_record}'s
     * optimistic lock — the entire reason {@code markDirty} no longer loads/saves the
     * aggregate (task 4.3). Deleted in task 4b.4.
     */
    @Modifying
    @Query("UPDATE sync.sync_record SET dirty_since = :dirtySince WHERE id = :id")
    void updateDirtySince(@Param("id") UUID id, @Param("dirtySince") Instant dirtySince);
}
