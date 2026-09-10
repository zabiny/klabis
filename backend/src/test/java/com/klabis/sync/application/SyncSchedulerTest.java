package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.domain.*;
import com.klabis.sync.fixtures.FixedClockTestSupport;
import com.klabis.sync.fixtures.MutableClock;
import com.klabis.sync.fixtures.TestAdapterConfiguration;
import com.klabis.sync.fixtures.TestSyncProjection;
import com.klabis.sync.fixtures.TestSynchronizationAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the two scheduled scan cadences (tasks.md 5.5): the full pass re-compares
 * every active record (skipping only {@code RETIRED} and {@code FAILED}, since D10
 * has terminally failed records "skipped by the scheduler") — including one already
 * in {@code CONFLICT}, which D7 has recomputed on every pass. The due scan touches
 * only dirty/retry-due records, which naturally skips retired, conflicted, terminally
 * failed and claimed ones (see {@code SyncRecordRepository.findDueForScan}). Both
 * stop the scan on an open circuit breaker, leaving the rest untouched (design.md
 * D11).
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, TestAdapterConfiguration.class})
@DisplayName("SyncScheduler")
class SyncSchedulerTest {

    @Autowired
    private SyncScheduler scheduler;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private SynchronizationAdapter synchronizationAdapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Replaces the production {@code java.time.Clock} bean (the one {@link SyncScheduler}
     * and {@link SyncRecordClaimer} inject) with a {@link MutableClock} the test body can
     * advance. The field is typed {@code Clock} to match the production bean it replaces;
     * {@link #mutableClock()} narrows it back for the {@code advanceBy} / {@code setInstant}
     * calls. {@code enforceOverride} makes a missing production bean a hard failure.
     */
    @TestBean(enforceOverride = true)
    private Clock clock;

    static Clock clock() {
        return FixedClockTestSupport.fixedClock();
    }

    private MutableClock mutableClock() {
        return (MutableClock) clock;
    }

    private TestSynchronizationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = (TestSynchronizationAdapter) synchronizationAdapter;
        adapter.reset();
        adapter.withCapabilities(SyncCapabilities.bidirectional());
        circuitBreakerRegistry.circuitBreaker(ResilientAdapterExecutor.INSTANCE_NAME).reset();
        mutableClock().setInstant(FixedClockTestSupport.FIXED_NOW);
    }

    private SyncRecord enrollAndSync(String entityId, String externalId) {
        adapter.withExternalState(externalId, new TestSyncProjection("Sprint", "Brno"));
        adapter.withLocalState(entityId, new TestSyncProjection("(unset)", "(unset)"));
        SyncTarget target = new SyncTarget(SyncEntityType.EVENT, entityId);
        ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, externalId);
        SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
        return synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
    }

    @Nested
    @DisplayName("full pass")
    class FullPass {

        @Test
        @DisplayName("re-compares every active record, including one already in conflict (design.md D7)")
        void reComparesEveryActiveRecordIncludingConflicted() {
            SyncRecord inSync = enrollAndSync("sched-full-1", "8300");

            adapter.withLocalState("sched-full-1", new TestSyncProjection("Local Edit", "Brno"));
            adapter.withExternalState("8300", new TestSyncProjection("External Edit", "Brno"));
            SyncRecord conflicted = synchronizationPort.synchronizeNow(inSync.getId(), "test-user");
            assertThat(conflicted.getStatus()).isEqualTo(SyncStatus.CONFLICT);

            // The external side now agrees with the local side again — a scheduled pass
            // must re-evaluate and clear the standing conflict (design.md D7).
            adapter.withExternalState("8300", new TestSyncProjection("Local Edit", "Brno"));

            scheduler.runFullPass();

            SyncRecord afterFullPass = synchronizationPort.state(inSync.getId());
            assertThat(afterFullPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        }

        @Test
        @DisplayName("stops the scan when the circuit breaker is open, leaving the remaining records untouched")
        void stopsOnOpenCircuitBreaker() {
            SyncRecord record = enrollAndSync("sched-full-2", "8301");
            circuitBreakerRegistry.circuitBreaker(ResilientAdapterExecutor.INSTANCE_NAME)
                    .transitionToForcedOpenState();

            adapter.withExternalState("8301", new TestSyncProjection("Sprint Updated", "Brno"));
            scheduler.runFullPass();

            SyncRecord untouched = synchronizationPort.state(record.getId());
            assertThat(untouched.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
        }

        @Test
        @DisplayName("skips a terminally failed record (design.md D10 — skipped by the scheduler until reset)")
        void skipsTerminallyFailedRecord() {
            SyncRecord record = enrollAndSync("sched-full-3", "8306");
            // Step off the initial SUCCESS row's instant so every failure that follows
            // sorts strictly after it: the derived failure count is read newest-first
            // and stops at the last SUCCESS/RESET row, so a failure sharing that
            // instant would be tie-ordered behind the boundary and never counted.
            mutableClock().advanceBy(Duration.ofDays(1));
            for (int i = 0; i < 5; i++) {
                circuitBreakerRegistry.circuitBreaker(ResilientAdapterExecutor.INSTANCE_NAME).reset();
                adapter.failNextReadExternalWith(3, new RetryableSyncFailureException("HTTP 503"));
                record = synchronizationPort.synchronizeNow(record.getId(), "test-user");
                // Each retryable failure reschedules with a growing backoff; advance
                // past it (a day clears even the 24h cap) so the next pass is genuinely
                // due and every attempt row lands at a distinct, increasing started_at —
                // the derived failure count is read from that ordering.
                mutableClock().advanceBy(Duration.ofDays(1));
            }
            assertThat(record.getStatus()).isEqualTo(SyncStatus.FAILED);
            circuitBreakerRegistry.circuitBreaker(ResilientAdapterExecutor.INSTANCE_NAME).reset();

            // If the scheduler ever attempted this record, runScheduledPass's own
            // Assert.state would throw and the per-record catch would just log it —
            // this asserts the record was never even handed to it in the first place,
            // not merely that no exception surfaced.
            scheduler.runFullPass();

            SyncRecord stillFailed = synchronizationPort.state(record.getId());
            assertThat(stillFailed.getStatus()).isEqualTo(SyncStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("due scan")
    class DueScan {

        @Test
        @DisplayName("picks up a dirty record and clears it")
        void picksUpDirtyRecord() {
            SyncRecord record = enrollAndSync("sched-due-1", "8302");
            SyncRecord marked = syncRecordRepository.findById(record.getId()).orElseThrow();
            marked.applyToSchedule(ScheduleEffect.dirtySince(clock.instant()));
            syncRecordRepository.save(marked);
            adapter.withExternalState("8302", new TestSyncProjection("Sprint Updated", "Brno"));

            scheduler.runDueScan();

            SyncRecord afterScan = synchronizationPort.state(record.getId());
            assertThat(afterScan.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint Updated", "Brno"));
        }

        @Test
        @DisplayName("skips a record that is neither dirty nor due")
        void skipsRecordNotDueYet() {
            SyncRecord record = enrollAndSync("sched-due-2", "8303");
            adapter.withExternalState("8303", new TestSyncProjection("Sprint Updated", "Brno"));

            scheduler.runDueScan();

            SyncRecord unchanged = synchronizationPort.state(record.getId());
            assertThat(unchanged.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
        }

        @Test
        @DisplayName("skips a claimed record still within its lease")
        void skipsFreshlyClaimedRecord() {
            SyncRecord record = enrollAndSync("sched-due-3", "8304");
            SyncRecord claimed = syncRecordRepository.findById(record.getId()).orElseThrow();
            claimed.applyToSchedule(ScheduleEffect.dirtySince(clock.instant()));
            claimed.claim(clock.instant());
            syncRecordRepository.save(claimed);
            adapter.withExternalState("8304", new TestSyncProjection("Sprint Updated", "Brno"));

            // Advance the clock, but stay inside the claim lease — the claim is still
            // fresh, so the due scan must skip this record.
            mutableClock().advanceBy(Duration.ofMinutes(1));

            scheduler.runDueScan();

            SyncRecord unchanged = synchronizationPort.state(record.getId());
            assertThat(unchanged.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
        }

        @Test
        @DisplayName("stops the scan when the circuit breaker is open, leaving the remaining records untouched")
        void stopsOnOpenCircuitBreaker() {
            SyncRecord record = enrollAndSync("sched-due-4", "8305");
            SyncRecord marked = syncRecordRepository.findById(record.getId()).orElseThrow();
            marked.applyToSchedule(ScheduleEffect.dirtySince(clock.instant()));
            syncRecordRepository.save(marked);

            circuitBreakerRegistry.circuitBreaker(ResilientAdapterExecutor.INSTANCE_NAME)
                    .transitionToForcedOpenState();
            adapter.withExternalState("8305", new TestSyncProjection("Sprint Updated", "Brno"));

            scheduler.runDueScan();

            SyncRecord untouched = synchronizationPort.state(record.getId());
            assertThat(untouched.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
        }
    }
}
