package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import com.klabis.sync.fixtures.TestAdapterConfiguration;
import com.klabis.sync.fixtures.TestSyncProjection;
import com.klabis.sync.fixtures.TestSynchronizationAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies the two structural guarantees left standing once the {@code markDirty} /
 * {@code SyncOutcomeWriter#persist} version race is gone (proposal.md tasks 6.2,
 * 6.3, 6.5): the outcome writer's transaction is still atomic across all three
 * tables it touches, the version-token short-circuit still depends on the schedule
 * being loaded with the record, and a claim can no longer be knocked over by a
 * concurrent {@code markDirty} the way it structurally could before scheduling moved
 * off {@code sync_record}.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, TestAdapterConfiguration.class})
class SynchronizationServiceTransactionBoundaryIntegrationTest {

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private SynchronizationAdapter synchronizationAdapter;

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private SyncAttemptRepository syncAttemptRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private SyncAttemptRepository spiedAttemptRepository;

    @MockitoSpyBean
    private SyncRecordRepository spiedRecordRepository;

    @MockitoSpyBean
    private SyncScheduleRepository spiedScheduleRepository;

    private TestSynchronizationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = (TestSynchronizationAdapter) synchronizationAdapter;
        adapter.reset();
        adapter.withCapabilities(SyncCapabilities.bidirectional());
    }

    @Nested
    @DisplayName("outcome writer atomicity (task 6.2, design.md D15)")
    class OutcomeWriterAtomicity {

        /**
         * A failure between the record save and the attempt append must roll back
         * both, plus the schedule write sitting between them — {@code persist} is one
         * plain {@code @Transactional} method (task 5.2 removed the
         * {@code REQUIRES_NEW} boundary that used to isolate them), so a JDBC failure
         * on the last of the three writes must undo the first two as well.
         * <p>
         * Verified to fail without its mechanism (task 6.7): temporarily replacing
         * {@code SyncOutcomeWriter#persist}'s {@code @Transactional} with none (or
         * with {@code Propagation.REQUIRES_NEW} on the attempt append alone, as it was
         * before task 5.2) leaves the record's {@code RETRYING} status and the
         * schedule's {@code next_attempt_due_at} committed even though the attempt
         * append still throws — this test then fails on both the status and the
         * schedule assertions below. Confirmed manually by re-adding
         * {@code Propagation.REQUIRES_NEW} to a throwaway copy of {@code doPersist}
         * and observing the assertions fail; not left in the tree since the
         * production method has no such seam to toggle without reintroducing the
         * layer task 5.2 removed.
         */
        @Test
        @DisplayName("a failure appending the attempt row rolls back the record save and the schedule write together")
        void attemptAppendFailureRollsBackRecordAndSchedule() {
            adapter.withExternalState("9300", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-atomicity-1", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-atomicity-1");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9300");
            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), null);

            long attemptCountBefore = attemptCount(enrolled.getId());

            // The next pass fails with a retryable error — record.recordRetryableFailure
            // returns a non-trivial ScheduleEffect (a dueAt), so a schedule write that
            // escapes the rollback is observable, not just a no-change effect that
            // would look identical whether it was rolled back or not.
            adapter.failNextReadExternalWith(3, new RetryableSyncFailureException("HTTP 503"));
            Mockito.doThrow(new RuntimeException("simulated attempt-append failure"))
                    .when(spiedAttemptRepository).save(Mockito.any());

            try {
                assertThatCode(() -> synchronizationPort.synchronizeNow(enrolled.getId(), null))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("simulated attempt-append failure");

                SyncRecord afterFailedPersist = syncRecordRepository.findById(enrolled.getId()).orElseThrow();
                assertThat(afterFailedPersist.getStatus())
                        .as("the record must still show its pre-pass status — the failed pass's status change never committed")
                        .isEqualTo(SyncStatus.IN_SYNC);
                assertThat(afterFailedPersist.getNextAttemptDueAt())
                        .as("the schedule write must have rolled back together with the record")
                        .isNull();
                assertThat(afterFailedPersist.getClaimedAt())
                        .as("releaseClaim() ran inside the same failed transaction as the record save and schedule write — "
                                + "the claim taken by SyncRecordClaimer (a separate, already-committed transaction) must still stand")
                        .isNotNull();
                assertThat(attemptCount(enrolled.getId())).isEqualTo(attemptCountBefore);
            } finally {
                Mockito.reset(spiedAttemptRepository);
            }
        }

        private long attemptCount(SyncRecordId id) {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sync.sync_attempt WHERE sync_record_id = ?", Long.class, id.value());
            return count != null ? count : 0L;
        }
    }

    @Nested
    @DisplayName("version-token short-circuit depends on the schedule being loaded with the record (task 6.3, 4.4)")
    class ShortCircuitDependsOnSchedule {

        /**
         * The short-circuit at {@code SynchronizationService.runPass} fires only when
         * {@code record.getDirtySince() == null}, and {@code getDirtySince} now reads
         * from the {@link SyncSchedule} loaded alongside the record (task 3.3), not a
         * field on the aggregate. This pins the condition end-to-end: a record with a
         * baseline and an explicit dirty marker must NOT short-circuit even though its
         * external version token is unchanged, because the marker means a local edit
         * was observed and the full read must run to pick it up.
         * <p>
         * Verified to fail without its mechanism (task 6.7): commenting out the
         * {@code record.getDirtySince() == null} conjunct in {@code runPass} (so the
         * short-circuit fires unconditionally whenever a baseline exists and the token
         * is unchanged) makes this test fail — the external read count would stay
         * unchanged instead of increasing, and the attempt would be SKIPPED instead of
         * carrying the dirty record's actual outcome. Verified manually by temporarily
         * deleting that conjunct and re-running; reverted immediately after, since
         * leaving a broken short-circuit in the tree just to keep a toggle would
         * reintroduce the exact silent-full-read-on-every-pass failure mode task 4.4
         * warns about.
         */
        @Test
        @DisplayName("a dirty record with an unchanged version token still runs the full read, not the short-circuit")
        void dirtyRecordWithUnchangedTokenSkipsShortCircuit() {
            adapter.withExternalState("9301", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-atomicity-2", new TestSyncProjection("Sprint", "Brno"));
            adapter.withVersionToken(new ExternalVersionToken("v1"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-atomicity-2");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9301");
            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), null);

            // Mark the record dirty without changing the version token: the cheap
            // indicator alone would normally short-circuit the next pass, but the
            // explicit dirty marker must override that.
            synchronizationPort.markDirty(target);
            adapter.withLocalState("event-atomicity-2", new TestSyncProjection("Sprint Updated", "Brno"));

            int externalReadsBeforeSecondPass = adapter.externalReadCount();
            SyncRecord afterSecondPass = synchronizationPort.synchronizeNow(enrolled.getId(), null);

            assertThat(adapter.externalReadCount())
                    .as("a dirty record must run the full read, not the version-token short-circuit")
                    .isGreaterThan(externalReadsBeforeSecondPass);
            var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(afterSecondPass.getId());
            assertThat(history.get(0).getOutcome()).isNotEqualTo(SyncOutcome.SKIPPED);
            assertThat(afterSecondPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint Updated", "Brno"));
        }
    }

    @Nested
    @DisplayName("conflict refresh is atomic across record and schedule (self-invocation regression)")
    class ConflictRefreshAtomicity {

        /**
         * {@code resolveConflict}'s refresh-on-moved-collision path (design.md D7)
         * saves the record's refreshed snapshots and clears its schedule together.
         * That write must not be split across two separately-committed transactions
         * — a crash between them would leave the record's refreshed snapshots
         * persisted while the schedule still carries a stale {@code dirtySince},
         * contradicting design.md D15's "every write of this kind is atomic".
         * <p>
         * The spy throws from {@code SyncScheduleRepository#apply}, the operation that
         * runs AFTER the record save in {@code SyncOutcomeWriter#persistConflictRefresh}
         * — throwing from the record save itself (an earlier attempt at this test did)
         * proves nothing, since a mocked {@code save} that throws never delegates to
         * the real one, so the record is never actually persisted regardless of
         * whether a transaction wraps the call at all.
         * <p>
         * Verified to fail without its mechanism (task 6.7 pattern): temporarily
         * changing the production code so the refresh path calls a package-private,
         * merely-{@code @Transactional} method on {@code SynchronizationService}
         * itself via {@code this.refreshConflict(...)} (the exact self-invocation
         * this test guards against, previously present at
         * {@code SynchronizationService#refreshConflict}) makes {@code @Transactional}
         * silently not apply — the call runs with no transaction at all, so the record
         * save the self-invoked method makes autocommits before the spy's exception is
         * thrown from the schedule apply that follows it. With that change restored,
         * this test failed on the {@code getExternal().projection()} assertion below:
         * it saw the refreshed external snapshot ("Sprint Yet Another External
         * Change") instead of the pre-refresh one, because the record save had
         * already committed on its own. Reverted immediately after confirming the
         * failure; the fix routes the refresh through {@link SyncOutcomeWriter}, a
         * genuine cross-bean call, instead.
         */
        @Test
        @DisplayName("a failure while refreshing a moved conflict rolls back the record save and the schedule clear together")
        void refreshFailureRollsBackRecordAndScheduleTogether() {
            adapter.withExternalState("9310", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-atomicity-4", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-atomicity-4");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9310");
            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-atomicity-4", new TestSyncProjection("Sprint Local Edit", "Brno"));
            adapter.withExternalState("9310", new TestSyncProjection("Sprint External Edit", "Brno"));
            SyncRecord conflicted = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            synchronizationPort.acknowledgeConflict(conflicted.getId(), "manager");

            // Mark the record dirty (without moving either side) so the schedule
            // carries a non-null dirtySince going into the refresh below — recordConflict
            // always applies ScheduleEffect.clear(), so a rolled-back schedule write is
            // only observable if there was something non-null to roll back to.
            synchronizationPort.markDirty(target);
            SyncRecord beforeRefresh = synchronizationPort.state(conflicted.getId());
            assertThat(beforeRefresh.getDirtySince()).isNotNull();

            // The external side moves again after acknowledgement but before resolution
            // — this is the moved-collision refresh path.
            adapter.withExternalState("9310", new TestSyncProjection("Sprint Yet Another External Change", "Brno"));

            Mockito.doThrow(new RuntimeException("simulated conflict-refresh failure"))
                    .when(spiedScheduleRepository).apply(Mockito.eq(conflicted.getId()), Mockito.any());

            try {
                assertThatCode(() -> synchronizationPort.resolveConflict(conflicted.getId(), SyncResolution.INWARD, "manager"))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("simulated conflict-refresh failure");
            } finally {
                Mockito.reset(spiedScheduleRepository);
            }

            SyncRecord afterFailedRefresh = syncRecordRepository.findById(conflicted.getId()).orElseThrow();
            assertThat(afterFailedRefresh.getExternal().projection())
                    .as("the record's refreshed external snapshot must not have committed — the whole refresh rolled back")
                    .isEqualTo(new TestSyncProjection("Sprint External Edit", "Brno"));
            assertThat(afterFailedRefresh.getDirtySince())
                    .as("the schedule clear must have rolled back together with the record save")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("claim no longer races a concurrent markDirty (task 6.5)")
    class ClaimDoesNotRaceMarkDirty {

        /**
         * Before scheduling moved off the aggregate, {@code SyncRecordClaimer.claim}'s
         * read-check-write on {@code sync_record} — {@code findById}, check the
         * lease, {@code claim(now)}, {@code save} — could be knocked over by a
         * concurrent {@code markDirty} that loaded, mutated and saved the very same
         * row in the window between the claim's read and its own save: {@code save}
         * would then throw {@code OptimisticLockingFailureException} instead of the
         * claim completing, even though no second pass was actually contending for
         * the claim. Now {@code markDirty} (task 4.3) never touches {@code sync_record}
         * at all, so this exact interleaving must complete cleanly.
         * <p>
         * The spy on {@link SyncRecordRepository#findById} pins the interleaving to
         * that window directly, the same technique
         * {@link SynchronizationServiceMarkDirtyIntegrationTest#passSaveSurvivesConcurrentMarkDirty}
         * uses for the outcome-writer side of the same removed race.
         * <p>
         * Verified to fail without its mechanism (task 6.7): confirmed manually by
         * temporarily reintroducing an aggregate-mutating {@code markDirty} (loading
         * the record, applying the schedule effect as today, then also calling
         * {@code syncRecordRepository.save(record)} — a stand-in for the version-
         * bumping write the old {@code SyncRecord.markDirty} made) in place of the
         * schedule-only write inside {@code markDirty}. With that reintroduced, and
         * this test's hook firing on the *second* {@code findById} call — the one
         * inside {@code SyncRecordClaimer.claim} itself, strictly before its own
         * {@code save} — {@code claimer.claim}'s save throws
         * {@code OptimisticLockingFailureException} and this test fails. (Firing on
         * the *first* {@code findById} call, {@code synchronizeNow}'s own CONFLICT/
         * FAILED precheck, does not reproduce it: the claimer's subsequent read
         * observes the version {@code markDirty} already bumped, so there is nothing
         * stale left for its save to lose against — the hook must target the
         * claimer's own read specifically.) Not left as a toggle in the tree:
         * reproducing it requires resurrecting the aggregate-mutating path task 4.3
         * deleted, which the fix this test guards against removed for good.
         */
        @Test
        @DisplayName("claim succeeds even when a markDirty on the same record runs concurrently between its read and its save")
        void claimSurvivesConcurrentMarkDirty() throws Exception {
            adapter.withExternalState("9302", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-atomicity-3", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-atomicity-3");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9302");
            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), null);

            adapter.withExternalState("9302", new TestSyncProjection("Sprint", "Praha"));

            CountDownLatch markDirtyDone = new CountDownLatch(1);
            // synchronizeNow reads the record twice before any write: once for its own
            // CONFLICT/FAILED precheck (getOrThrow), then again inside
            // SyncRecordClaimer.claim itself, strictly before the claimer's save. Only
            // the second read is the window this test targets — firing on the first
            // would let the concurrent markDirty's write (were it to touch sync_record)
            // be visible to the claimer's own subsequent read, leaving nothing stale
            // for the claimer's save to race against.
            java.util.concurrent.atomic.AtomicInteger findByIdCallCount = new java.util.concurrent.atomic.AtomicInteger(0);
            try {
                Mockito.doAnswer(invocation -> {
                    Object result = invocation.callRealMethod();
                    if (findByIdCallCount.incrementAndGet() == 2) {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        try {
                            Future<?> future = executor.submit(() -> synchronizationPort.markDirty(target));
                            future.get(5, TimeUnit.SECONDS);
                        } finally {
                            executor.shutdown();
                        }
                        markDirtyDone.countDown();
                    }
                    return result;
                }).when(spiedRecordRepository).findById(enrolled.getId());

                assertThatCode(() -> synchronizationPort.synchronizeNow(enrolled.getId(), null))
                        .doesNotThrowAnyException();

                assertThat(markDirtyDone.getCount()).isZero();
            } finally {
                Mockito.reset(spiedRecordRepository);
            }
        }
    }
}
