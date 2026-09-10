package com.klabis.sync.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.ScheduleEffect;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
import com.klabis.sync.domain.SyncSchedule;
import com.klabis.sync.domain.SyncScheduleRepository;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SyncScheduleRepositoryAdapter} (proposal.md task 2.2, 2.5, 2.6) — the new
 * {@code sync_schedule} table, purely additive at this point: nothing in production
 * code yet reads or writes it, so this test is the only thing exercising it.
 */
@DisplayName("SyncSchedule JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Import(SyncProjectionTypeTestConfiguration.class)
class SyncScheduleRepositoryAdapterTest {

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private SyncScheduleRepository syncScheduleRepository;

    @Autowired
    private SyncScheduleJdbcRepository jdbcRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    private SyncRecordId enrollRecord(String entityId, String externalId) {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, entityId),
                new ExternalReference(ExternalSystem.ORIS, externalId));
        return syncRecordRepository.save(record).getId();
    }

    @Nested
    @DisplayName("findByRecordId()")
    class FindByRecordId {

        @Test
        @DisplayName("returns an empty schedule for a record with no schedule row (task 2.6 coded default)")
        void returnsEmptyScheduleWhenNoRowExists() {
            SyncRecordId recordId = enrollRecord("schedule-1", "9301");

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);

            assertThat(schedule).isEqualTo(SyncSchedule.empty());
        }
    }

    @Nested
    @DisplayName("createFor()")
    class CreateFor {

        @Test
        @DisplayName("creates an empty schedule row for a newly enrolled record")
        void createsEmptyScheduleRow() {
            SyncRecordId recordId = enrollRecord("schedule-2", "9302");

            syncScheduleRepository.createFor(recordId);

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince()).isNull();
            assertThat(schedule.nextAttemptDueAt()).isNull();
        }
    }

    @Nested
    @DisplayName("apply()")
    class Apply {

        @Test
        @DisplayName("dirtySince(instant) sets dirtySince on a freshly created schedule row")
        void appliesDirtySinceEffect() {
            SyncRecordId recordId = enrollRecord("schedule-3", "9303");
            syncScheduleRepository.createFor(recordId);
            Instant now = Instant.now();

            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(now));

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(now.truncatedTo(ChronoUnit.MILLIS));
            assertThat(schedule.nextAttemptDueAt()).isNull();
        }

        @Test
        @DisplayName("apply() works even without a prior createFor() call — falls back to the empty default before applying")
        void appliesEffectWithoutPriorCreate() {
            SyncRecordId recordId = enrollRecord("schedule-4", "9304");
            Instant dueAt = Instant.now().plusSeconds(900);

            syncScheduleRepository.apply(recordId, ScheduleEffect.dueAt(dueAt));

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.nextAttemptDueAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(dueAt.truncatedTo(ChronoUnit.MILLIS));
        }

        @Test
        @DisplayName("clear() clears both fields after dirtySince and dueAt were both set")
        void clearClearsBothFields() {
            SyncRecordId recordId = enrollRecord("schedule-5", "9305");
            syncScheduleRepository.createFor(recordId);
            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(Instant.now()));
            syncScheduleRepository.apply(recordId, ScheduleEffect.dueAt(Instant.now().plusSeconds(60)));

            syncScheduleRepository.apply(recordId, ScheduleEffect.clear());

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule).isEqualTo(SyncSchedule.empty());
        }

        @Test
        @DisplayName("clearDueAt() clears only nextAttemptDueAt, leaving dirtySince standing")
        void clearDueAtLeavesDirtySinceStanding() {
            SyncRecordId recordId = enrollRecord("schedule-6", "9306");
            syncScheduleRepository.createFor(recordId);
            Instant dirtySince = Instant.now();
            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(dirtySince));
            syncScheduleRepository.apply(recordId, ScheduleEffect.dueAt(Instant.now().plusSeconds(60)));

            syncScheduleRepository.apply(recordId, ScheduleEffect.clearDueAt());

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(dirtySince.truncatedTo(ChronoUnit.MILLIS));
            assertThat(schedule.nextAttemptDueAt()).isNull();
        }

        @Test
        @DisplayName("dirtySince(instant) does not move an already-set marker (first-writer-wins, oldest-undelivered-change semantics)")
        void secondDirtySinceDoesNotMoveExistingMarker() {
            SyncRecordId recordId = enrollRecord("schedule-10", "9310");
            syncScheduleRepository.createFor(recordId);
            Instant first = Instant.now().minusSeconds(60);

            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(first));
            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(Instant.now()));

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince().truncatedTo(ChronoUnit.MILLIS))
                    .as("the first dirtySince marker must win over a later one — it records when the burst of unsynced changes started")
                    .isEqualTo(first.truncatedTo(ChronoUnit.MILLIS));
        }

        @Test
        @DisplayName("dirtySince(instant) sets a fresh marker again after clear() (COALESCE must not stick forever)")
        void dirtySinceCanBeSetAgainAfterClear() {
            SyncRecordId recordId = enrollRecord("schedule-11", "9311");
            syncScheduleRepository.createFor(recordId);
            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(Instant.now().minusSeconds(60)));
            syncScheduleRepository.apply(recordId, ScheduleEffect.clear());
            Instant newMarker = Instant.now();

            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(newMarker));

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince().truncatedTo(ChronoUnit.MILLIS))
                    .as("after clear() the record must be markable dirty again, otherwise it would never resync")
                    .isEqualTo(newMarker.truncatedTo(ChronoUnit.MILLIS));
        }

        @Test
        @DisplayName("repeated applies persist across separate calls (read-modify-write, no version conflict)")
        void repeatedAppliesPersistAcrossCalls() {
            SyncRecordId recordId = enrollRecord("schedule-7", "9307");
            syncScheduleRepository.createFor(recordId);

            for (int i = 0; i < 5; i++) {
                syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(Instant.now()));
            }

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince()).isNotNull();
        }

        /**
         * Proves the simplify-review finding that a read-modify-write {@code apply()}
         * loses a concurrent writer's column: two effects that touch <em>different</em>
         * columns ({@code DIRTY_SINCE} and {@code DUE_AT}) must both survive even when
         * their read-modify-write cycles interleave — {@code sync_schedule} carries no
         * version to protect against this the way {@code sync_record} would.
         * <p>
         * {@code readModifyWriteApply} below reproduces the exact shape the pre-fix
         * {@code SyncScheduleRepositoryAdapter.apply} used (read the row in Java,
         * compute the next {@link SyncSchedule}, save the whole row) rather than
         * calling {@link SyncScheduleRepository#apply}, which is now the fixed,
         * single-column-{@code UPDATE} implementation and cannot be driven through this
         * interleaving at all — there is no read step left to pin. The interleaving is
         * forced explicitly, not hoped for via thread timing: B reads first and
         * completes its whole read-modify-write, then A (whose own read was pinned
         * *before* B's write, via the latch) saves last, carrying its stale pre-image
         * of {@code nextAttemptDueAt} (null) over B's committed value. Per the
         * project's negative-test rule, this reproduction was verified to fail before
         * the fix — {@code nextAttemptDueAt} came back null on the read-modify-write
         * path — and this test still exercises that same losing mechanism directly (it
         * does not depend on which implementation {@code SyncScheduleRepository.apply}
         * currently has), so it stays valid as regression coverage against ever
         * reintroducing read-modify-write here.
         */
        @Test
        @DisplayName("a read-modify-write apply() loses a concurrent writer's column (regression pin for the fixed apply())")
        void readModifyWriteApplyLosesConcurrentColumn() throws Exception {
            SyncRecordId recordId = enrollRecord("schedule-8", "9308");
            syncScheduleRepository.createFor(recordId);
            Instant dueAt = Instant.now().plusSeconds(900);
            Instant dirtySince = Instant.now();

            // @DataJdbcTest wraps each test in an uncommitted transaction by default;
            // the worker threads below run on separate connections and would not see
            // this test's own setup writes above without actually committing them
            // first.
            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();

            CountDownLatch aHasRead = new CountDownLatch(1);
            CountDownLatch bHasWritten = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                // Thread A: reads the pre-image (still empty) *before* B writes, then
                // waits for B's write to commit, then saves its own effect —
                // overwriting B's column with the stale pre-image.
                Future<?> writerA = executor.submit(() -> {
                    SyncSchedule aPreImage = jdbcRepository.findById(recordId.value())
                            .map(SyncScheduleMemento::toSyncSchedule)
                            .orElseGet(SyncSchedule::empty);
                    aHasRead.countDown();
                    awaitUninterruptibly(bHasWritten);
                    readModifyWriteApply(recordId, aPreImage, ScheduleEffect.dirtySince(dirtySince));
                });
                Future<?> writerB = executor.submit(() -> {
                    awaitUninterruptibly(aHasRead);
                    SyncSchedule bPreImage = jdbcRepository.findById(recordId.value())
                            .map(SyncScheduleMemento::toSyncSchedule)
                            .orElseGet(SyncSchedule::empty);
                    readModifyWriteApply(recordId, bPreImage, ScheduleEffect.dueAt(dueAt));
                    bHasWritten.countDown();
                });
                writerB.get(5, TimeUnit.SECONDS);
                writerA.get(5, TimeUnit.SECONDS);
            } finally {
                executor.shutdown();
            }

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.nextAttemptDueAt())
                    .as("read-modify-write loses B's DUE_AT to A's stale pre-image save — this is the bug the fix removes")
                    .isNull();
        }

        /**
         * Positive counterpart to {@link #readModifyWriteApplyLosesConcurrentColumn}:
         * proves the fixed {@link SyncScheduleRepository#apply} does not lose a
         * concurrent writer's column under the very interleaving that broke the old
         * read-modify-write path. The fixed {@code apply()} has no read step to pin a
         * race on, so the interleaving here is forced by call order instead — B's
         * {@code apply()} runs to completion first, then A's — which is exactly the
         * order that made the pre-fix reproduction above lose B's {@code DUE_AT} to
         * A's stale pre-image. With the fixed single-column {@code UPDATE}, order
         * between the two columns cannot matter, so both {@code dirtySince} and
         * {@code nextAttemptDueAt} must survive regardless of which one runs last.
         */
        @Test
        @DisplayName("apply() does not lose a concurrent writer's column (fix for readModifyWriteApplyLosesConcurrentColumn)")
        void concurrentApplyOnDifferentColumnsBothSurvive() throws Exception {
            SyncRecordId recordId = enrollRecord("schedule-9", "9309");
            syncScheduleRepository.createFor(recordId);
            Instant dueAt = Instant.now().plusSeconds(900);
            Instant dirtySince = Instant.now();

            // @DataJdbcTest wraps each test in an uncommitted transaction by default;
            // the worker threads below run on separate connections and would not see
            // this test's own setup writes above without actually committing them
            // first.
            TestTransaction.flagForCommit();
            TestTransaction.end();
            TestTransaction.start();

            CountDownLatch aHasRead = new CountDownLatch(1);
            CountDownLatch bHasWritten = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                // Thread A: waits for B's apply() to complete first, then applies its
                // own effect — the same B-before-A order that lost B's column on the
                // read-modify-write path above.
                Future<?> writerA = executor.submit(() -> {
                    aHasRead.countDown();
                    awaitUninterruptibly(bHasWritten);
                    syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(dirtySince));
                });
                Future<?> writerB = executor.submit(() -> {
                    awaitUninterruptibly(aHasRead);
                    syncScheduleRepository.apply(recordId, ScheduleEffect.dueAt(dueAt));
                    bHasWritten.countDown();
                });
                writerB.get(5, TimeUnit.SECONDS);
                writerA.get(5, TimeUnit.SECONDS);
            } finally {
                executor.shutdown();
            }

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince().truncatedTo(ChronoUnit.MILLIS))
                    .as("A's DIRTY_SINCE column update must survive")
                    .isEqualTo(dirtySince.truncatedTo(ChronoUnit.MILLIS));
            assertThat(schedule.nextAttemptDueAt().truncatedTo(ChronoUnit.MILLIS))
                    .as("B's DUE_AT column update must survive, unlike the read-modify-write path")
                    .isEqualTo(dueAt.truncatedTo(ChronoUnit.MILLIS));
        }

        /**
         * The pre-fix {@code SyncScheduleRepositoryAdapter.apply} shape, reconstructed
         * here only to pin the interleaving above — production code no longer contains
         * this logic (see {@link SyncScheduleRepositoryAdapter#apply}). Writes both
         * columns of {@code next} in one {@code UPDATE}, exactly as the old
         * {@code CrudRepository.save(SyncScheduleMemento)} did: the whole row, not just
         * the column {@code effect} touched — which is exactly how B's column gets
         * overwritten by A's stale pre-image.
         */
        private void readModifyWriteApply(SyncRecordId recordId, SyncSchedule preImage, ScheduleEffect effect) {
            SyncSchedule next = preImage.apply(effect);
            jdbcTemplate.update("""
                    UPDATE sync.sync_schedule
                    SET dirty_since = :dirtySince, next_attempt_due_at = :nextAttemptDueAt
                    WHERE sync_record_id = :recordId
                    """,
                    new MapSqlParameterSource()
                            .addValue("dirtySince", next.dirtySince())
                            .addValue("nextAttemptDueAt", next.nextAttemptDueAt())
                            .addValue("recordId", recordId.value()));
        }

        private void awaitUninterruptibly(CountDownLatch latch) {
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
