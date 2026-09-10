package com.klabis.sync.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Each {@code updateXxx}/{@code upsertXxx} method is an unconditional write over only
 * the columns a {@link com.klabis.sync.domain.ScheduleEffect} touches — deliberately
 * not a read-modify-write (proposal.md task 2.3, and the simplify review that replaced
 * the original read-modify-write {@code SyncScheduleRepositoryAdapter.apply}): two
 * concurrent effects on different fields (e.g. {@code markDirty}'s {@code DIRTY_SINCE}
 * racing {@code SyncOutcomeWriter}'s {@code DUE_AT}) must not read each other's
 * pre-image and overwrite one another's column. The database row lock taken by each
 * statement is the only synchronisation needed; {@code sync_schedule} still carries no
 * version column, so there is nothing to retry.
 * <p>
 * {@code CLEAR}/{@code CLEAR_DUE_AT} stay plain {@code UPDATE}s: a missing row already
 * behaves like {@link com.klabis.sync.domain.SyncSchedule#empty()} (task 2.6's coded
 * default), so a no-op {@code UPDATE} against one is the correct outcome. {@code
 * DUE_AT}/{@code DIRTY_SINCE} set a non-null value that must survive even without a
 * prior {@code createFor()} row (task 2.6). A single-statement upsert (H2's own
 * {@code MERGE}, or PostgreSQL's {@code INSERT ... ON CONFLICT}, which H2 in
 * {@code MODE=PostgreSQL} does not accept) takes a table-level lock on H2's MVStore
 * engine and made two genuinely concurrent single-column effects on the same row throw
 * {@code Concurrent update} instead of serialising through the row lock a plain
 * {@code UPDATE} takes — exactly the failure mode this change exists to remove. So
 * {@code updateXxx}/{@code insertIfMissingXxx} is a two-statement upsert instead: try
 * the {@code UPDATE} first (the steady-state path — every record has a row from
 * enrolment, task 2.6/4.7), and only on 0 rows affected does {@link
 * SyncScheduleRepositoryAdapter} fall back to an {@code INSERT}. The adapter's
 * try-update-then-insert is itself racy against a concurrent {@code createFor()} for
 * the *same still-unenrolled* record, but that is a can't-happen: {@code createFor} is
 * called once, synchronously, inside {@code enroll}'s own transaction, before the
 * record (and so any {@code ScheduleEffect}) can reach any other caller.
 */
@Repository
interface SyncScheduleJdbcRepository extends CrudRepository<SyncScheduleMemento, UUID> {

    @Modifying
    @Query("""
            UPDATE sync.sync_schedule
            SET dirty_since = NULL, next_attempt_due_at = NULL
            WHERE sync_record_id = :recordId
            """)
    int updateClear(@Param("recordId") UUID recordId);

    @Modifying
    @Query("""
            UPDATE sync.sync_schedule
            SET next_attempt_due_at = NULL
            WHERE sync_record_id = :recordId
            """)
    int updateClearDueAt(@Param("recordId") UUID recordId);

    @Modifying
    @Query("""
            UPDATE sync.sync_schedule
            SET next_attempt_due_at = :dueAt
            WHERE sync_record_id = :recordId
            """)
    int updateDueAt(@Param("recordId") UUID recordId, @Param("dueAt") Instant dueAt);

    @Modifying
    @Query("""
            UPDATE sync.sync_schedule
            SET dirty_since = :dirtySince
            WHERE sync_record_id = :recordId
            """)
    int updateDirtySince(@Param("recordId") UUID recordId, @Param("dirtySince") Instant dirtySince);

    @Modifying
    @Query("""
            INSERT INTO sync.sync_schedule (sync_record_id, next_attempt_due_at)
            VALUES (:recordId, :dueAt)
            """)
    void insertIfMissingDueAt(@Param("recordId") UUID recordId, @Param("dueAt") Instant dueAt);

    @Modifying
    @Query("""
            INSERT INTO sync.sync_schedule (sync_record_id, dirty_since)
            VALUES (:recordId, :dirtySince)
            """)
    void insertIfMissingDirtySince(@Param("recordId") UUID recordId, @Param("dirtySince") Instant dirtySince);
}
