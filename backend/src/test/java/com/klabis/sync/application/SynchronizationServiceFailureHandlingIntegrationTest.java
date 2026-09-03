package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import com.klabis.sync.fixtures.TestSyncProjection;
import com.klabis.sync.fixtures.TestSynchronizationAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end tests for retry, terminal failure, outage and claim handling (tasks.md
 * 4.9), driven entirely through {@link TestSynchronizationAdapter} — no ORIS
 * involvement (design.md Migration Plan, step 1).
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, SynchronizationServiceFailureHandlingIntegrationTest.TestAdapterConfiguration.class})
@DisplayName("Synchronisation engine: retry, terminal failure, outage and claim handling")
class SynchronizationServiceFailureHandlingIntegrationTest {

    @TestConfiguration
    static class TestAdapterConfiguration {
        @Bean
        SynchronizationAdapter testSynchronizationAdapter() {
            return new TestSynchronizationAdapter(SyncEntityType.EVENT, ExternalSystem.ORIS);
        }
    }

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private SynchronizationAdapter synchronizationAdapter;

    @Autowired
    private SyncAttemptRepository syncAttemptRepository;

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private TestSynchronizationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = (TestSynchronizationAdapter) synchronizationAdapter;
        adapter.reset();
        adapter.withCapabilities(new SyncCapabilities(true, true, true, true, false, false, false));
        // The sync-adapter circuit breaker is a Spring singleton shared across every
        // test method in this class (they reuse one Spring context) — reset it via the
        // ordinary Resilience4j API so one test's induced failures never leave the
        // breaker open for the next. No test-only method on the production class.
        resetCircuitBreaker();
    }

    private void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker(ResilientAdapterExecutor.INSTANCE_NAME).reset();
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
    @DisplayName("retry and terminal failure")
    class RetryAndTerminalFailure {

        @Test
        @DisplayName("a repeatedly failing record enters RETRYING with a growing due date, then FAILED at the limit")
        void repeatedFailure_retryingThenFailed() {
            SyncRecord record = enrollAndSync("event-200", "8200");
            assertThat(record.getStatus()).isEqualTo(SyncStatus.IN_SYNC);

            Instant previousDueAt = null;
            for (int attemptNumber = 1; attemptNumber <= 5; attemptNumber++) {
                // Reset the breaker before each simulated pass: this test simulates one
                // record persistently failing on its own (design.md D10's termination
                // case), not a systemic external-system outage (D11) — the two share the
                // same sync-adapter breaker instance, and enough in-attempt retries
                // across iterations would otherwise trip it for reasons unrelated to
                // what this test means to exercise.
                resetCircuitBreaker();
                // 3 failures: exhausts the sync-adapter Resilience4j in-attempt retry
                // budget (application.yml, max-attempts: 3) so this becomes one
                // genuinely failed pass-level attempt, not one absorbed transparently.
                adapter.failNextReadExternalWith(3, new RetryableSyncFailureException("HTTP 503"));
                record = synchronizationPort.synchronizeNow(record.getId(), "test-user");

                if (attemptNumber < 5) {
                    assertThat(record.getStatus()).isEqualTo(SyncStatus.RETRYING);
                    assertThat(record.getNextAttemptDueAt()).isNotNull();
                    if (previousDueAt != null) {
                        assertThat(record.getNextAttemptDueAt()).isAfter(previousDueAt);
                    }
                    previousDueAt = record.getNextAttemptDueAt();
                }
            }

            assertThat(record.getStatus()).isEqualTo(SyncStatus.FAILED);
            assertThat(record.getNextAttemptDueAt()).isNull();
        }

        @Test
        @DisplayName("reset restarts a terminally failed record and it synchronises again")
        void resetRestartsRecord() {
            SyncRecord record = enrollAndSync("event-201", "8201");
            for (int i = 0; i < 5; i++) {
                resetCircuitBreaker();
                adapter.failNextReadExternalWith(3, new RetryableSyncFailureException("HTTP 503"));
                record = synchronizationPort.synchronizeNow(record.getId(), "test-user");
            }
            assertThat(record.getStatus()).isEqualTo(SyncStatus.FAILED);

            SyncRecord afterReset = synchronizationPort.reset(record.getId(), "manager");
            assertThat(afterReset.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(record.getId());
            assertThat(history.get(0).getOutcome()).isEqualTo(SyncOutcome.RESET);

            resetCircuitBreaker();
            adapter.withExternalState("8201", new TestSyncProjection("Sprint Updated", "Brno"));
            SyncRecord afterNextPass = synchronizationPort.synchronizeNow(afterReset.getId(), "test-user");
            assertThat(afterNextPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            assertThat(afterNextPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint Updated", "Brno"));
        }

        @Test
        @DisplayName("reset on a record that is not failed is refused")
        void resetOnNonFailedRecordRefused() {
            SyncRecord record = enrollAndSync("event-202", "8202");

            assertThatThrownBy(() -> synchronizationPort.reset(record.getId(), "manager"))
                    .isInstanceOf(SyncRecordNotFailedException.class);
        }

        @Test
        @DisplayName("a terminal (non-retryable) failure fails the record on the very first attempt")
        void terminalFailureFailsImmediately() {
            SyncRecord record = enrollAndSync("event-203", "8203");
            adapter.failNextReadExternalWith(new IllegalArgumentException("malformed projection"));

            SyncRecord afterFailure = synchronizationPort.synchronizeNow(record.getId(), "test-user");

            assertThat(afterFailure.getStatus()).isEqualTo(SyncStatus.FAILED);
            var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(record.getId());
            assertThat(history.get(0).getOutcome()).isEqualTo(SyncOutcome.FAILED);
        }
    }

    @Nested
    @DisplayName("outage")
    class Outage {

        @Test
        @DisplayName("outage failures reschedule at the initial delay and never terminate the record")
        void outageDoesNotTerminate() {
            SyncRecord record = enrollAndSync("event-204", "8204");

            for (int i = 0; i < 10; i++) {
                adapter.failNextReadExternalWith(new java.io.UncheckedIOException(new java.net.SocketTimeoutException("read timed out")));
                record = synchronizationPort.synchronizeNow(record.getId(), "test-user");
                assertThat(record.getStatus()).isEqualTo(SyncStatus.RETRYING);
            }

            var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(record.getId());
            // The first attempt (from enrollAndSync's initial adoption) is SUCCESS; the
            // ten outage failures that follow must all be OUTAGE — none FAILED, and the
            // record above never moved to FAILED even after ten failures in a row.
            assertThat(history).hasSize(11);
            assertThat(history.subList(0, 10)).extracting(SyncAttempt::getOutcome).containsOnly(SyncOutcome.OUTAGE);
            assertThat(history.get(10).getOutcome()).isEqualTo(SyncOutcome.SUCCESS);
        }
    }

    @Nested
    @DisplayName("claim")
    class Claim {

        @Test
        @DisplayName("an expired claim is picked up by the next pass")
        void expiredClaimPickedUpAgain() {
            SyncRecord record = enrollAndSync("event-205", "8205");

            SyncRecord claimed = syncRecordRepository.findById(record.getId()).orElseThrow();
            claimed.claim(Instant.now().minus(java.time.Duration.ofHours(1)));
            syncRecordRepository.save(claimed);

            adapter.withExternalState("8205", new TestSyncProjection("Sprint Updated", "Brno"));
            SyncRecord afterPass = synchronizationPort.synchronizeNow(record.getId(), "test-user");

            assertThat(afterPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint Updated", "Brno"));
        }

        @Test
        @DisplayName("a fresh claim is skipped by a concurrent pass, other records remain available")
        void freshClaimSkipsConcurrentPass() {
            SyncRecord record = enrollAndSync("event-206", "8206");

            SyncRecord claimed = syncRecordRepository.findById(record.getId()).orElseThrow();
            claimed.claim(Instant.now());
            syncRecordRepository.save(claimed);

            assertThatThrownBy(() -> synchronizationPort.synchronizeNow(record.getId(), "test-user"))
                    .isInstanceOf(SyncRecordClaimedException.class);

            // A different record, freshly enrolled and unclaimed, is unaffected.
            SyncRecord otherRecord = enrollAndSync("event-207", "8207");
            assertThat(otherRecord.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        }
    }

    @Nested
    @DisplayName("failedAttemptsSinceLastSuccess (tasks.md 6.6, backs getSyncState's response)")
    class FailedAttemptsSinceLastSuccess {

        @Test
        @DisplayName("is zero right after enrolment succeeds")
        void zeroAfterSuccess() {
            SyncRecord record = enrollAndSync("event-210", "8210");

            assertThat(synchronizationPort.failedAttemptsSinceLastSuccess(record.getId())).isZero();
        }

        @Test
        @DisplayName("counts retryable failures since the last success, and resets to zero after the next success")
        void countsFailuresThenResetsOnSuccess() {
            SyncRecord record = enrollAndSync("event-211", "8211");

            resetCircuitBreaker();
            adapter.failNextReadExternalWith(3, new RetryableSyncFailureException("HTTP 503"));
            synchronizationPort.synchronizeNow(record.getId(), "test-user");

            assertThat(synchronizationPort.failedAttemptsSinceLastSuccess(record.getId())).isEqualTo(1);

            resetCircuitBreaker();
            adapter.withExternalState("8211", new TestSyncProjection("Sprint Updated", "Brno"));
            synchronizationPort.synchronizeNow(record.getId(), "test-user");

            assertThat(synchronizationPort.failedAttemptsSinceLastSuccess(record.getId())).isZero();
        }
    }

    @Nested
    @DisplayName("synchronizeNow rejects records needing a decision")
    class SynchronizeNowRejectsStuckRecords {

        @Test
        @DisplayName("synchronizeNow on a FAILED record is refused, not silently returned to service")
        void synchronizeNowRefusedOnFailedRecord() {
            SyncRecord record = enrollAndSync("event-208", "8208");
            for (int i = 0; i < 5; i++) {
                resetCircuitBreaker();
                adapter.failNextReadExternalWith(3, new RetryableSyncFailureException("HTTP 503"));
                record = synchronizationPort.synchronizeNow(record.getId(), "test-user");
            }
            assertThat(record.getStatus()).isEqualTo(SyncStatus.FAILED);
            long historySizeBeforeAttempt = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(record.getId()).size();

            SyncRecordId recordId = record.getId();
            assertThatThrownBy(() -> synchronizationPort.synchronizeNow(recordId, "test-user"))
                    .isInstanceOf(SyncRecordNeedsResolutionException.class);

            // No attempt was recorded, and the record is still FAILED — not silently
            // re-synchronised and returned to IN_SYNC without a RESET row.
            SyncRecord stillFailed = synchronizationPort.state(recordId);
            assertThat(stillFailed.getStatus()).isEqualTo(SyncStatus.FAILED);
            assertThat(syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(recordId)).hasSize((int) historySizeBeforeAttempt);
        }

        @Test
        @DisplayName("synchronizeNow on a CONFLICT record is refused")
        void synchronizeNowRefusedOnConflictRecord() {
            adapter.withExternalState("8209", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-209", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-209");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8209");
            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-209", new TestSyncProjection("Local Edit", "Brno"));
            adapter.withExternalState("8209", new TestSyncProjection("External Edit", "Brno"));
            SyncRecord conflicted = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            assertThat(conflicted.getStatus()).isEqualTo(SyncStatus.CONFLICT);

            assertThatThrownBy(() -> synchronizationPort.synchronizeNow(enrolled.getId(), "test-user"))
                    .isInstanceOf(SyncRecordNeedsResolutionException.class);

            SyncRecord stillConflicted = synchronizationPort.state(enrolled.getId());
            assertThat(stillConflicted.getStatus()).isEqualTo(SyncStatus.CONFLICT);
        }
    }
}
