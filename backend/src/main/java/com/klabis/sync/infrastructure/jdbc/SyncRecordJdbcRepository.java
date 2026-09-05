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
     * {@code retired_at IS NULL}. {@code CONFLICT} self-excludes: {@code recordConflict}
     * clears both {@code dirty_since} and {@code next_attempt_due_at}, so a conflicted
     * record matches neither dirty nor retry-due. {@code FAILED} does <strong>not</strong>
     * self-exclude — {@code recordTerminalFailure} clears only {@code next_attempt_due_at}
     * and leaves {@code dirty_since} untouched, so a terminally failed record that a
     * later edit marked dirty would otherwise be selected here and then rejected by
     * {@code SyncRecord.assertBeingAttempted} once {@code runScheduledPass} tried it. The
     * {@code status <> 'FAILED'} predicate keeps that exclusion local to the query,
     * matching what {@link #findAllActive} already does for the nightly full pass (see
     * {@link com.klabis.sync.domain.SyncRecordRepository#findDueForScan}). One indexed
     * query, matching the index on {@code (dirty_since, next_attempt_due_at)} declared
     * in the schema.
     */
    @Query("""
            SELECT * FROM sync.sync_record
            WHERE retired_at IS NULL
              AND status <> 'FAILED'
              AND (dirty_since IS NOT NULL OR next_attempt_due_at <= :now)
              AND (claimed_at IS NULL OR claimed_at <= :claimStaleBefore)
            """)
    List<SyncRecordMemento> findDueForScan(@Param("now") Instant now, @Param("claimStaleBefore") Instant claimStaleBefore);
}
