package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.domain.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * design.md "Domain Changes", D1-D4, D8, D9: {@link SynchronizationPort#pullAndEnroll}
 * brings in a record Klabis does not have — creates the local entity, pairs it, and
 * runs the initial pass, all driven through {@link TestSynchronizationAdapter}, no
 * ORIS involvement (Migration Plan step 1).
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, TestAdapterConfiguration.class})
@DisplayName("pullAndEnroll: bringing in a record Klabis does not have")
class SynchronizationServicePullAndEnrollIntegrationTest {

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private SynchronizationService synchronizationService;

    @Autowired
    private SynchronizationAdapter synchronizationAdapter;

    @Autowired
    private SyncAttemptRepository syncAttemptRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private TestSynchronizationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = (TestSynchronizationAdapter) synchronizationAdapter;
        adapter.reset();
        adapter.withCapabilities(SyncCapabilities.pullOnlyCreating());
        // The circuit breaker is a Spring singleton shared across every test method in
        // this class (they reuse one Spring context) — reset it so a previous test's
        // induced failures cannot open the breaker for this one (same pattern as
        // SynchronizationServiceFailureHandlingIntegrationTest).
        resetCircuitBreaker();
    }

    private void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker(ResilientAdapterExecutor.INSTANCE_NAME).reset();
    }

    @Test
    @DisplayName("creates the local entity, pairs it, establishes a baseline, and records exactly one attempt carrying the acting user")
    void pullAndEnroll_noExistingPairing_createsPairsAndEstablishesBaseline() {
        adapter.withExternalState("9200", new TestSyncProjection("Sprint", "Brno"));
        ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9200");

        SyncRecord result = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");

        assertThat(result.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(result.getExternalReference()).isEqualTo(externalRef);
        assertThat(result.getBaseline()).isNotNull();
        assertThat(result.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));

        var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(result.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getActingUser()).isEqualTo("test-user");
    }

    @Test
    @DisplayName("an adapter that does not declare createsLocal refuses the operation and creates nothing")
    void pullAndEnroll_adapterDoesNotDeclareCreatesLocal_refused() {
        adapter.withCapabilities(SyncCapabilities.pullOnly());
        adapter.withExternalState("9201", new TestSyncProjection("Sprint", "Brno"));
        ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9201");

        assertThatThrownBy(() -> synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT))
                .noneMatch(record -> record.getExternalReference().equals(externalRef));
        assertThat(adapter.createLocalCallCount()).isZero();
    }

    @Test
    @DisplayName("the external system cannot be read: no entity is created and no pairing remains")
    void pullAndEnroll_externalReadFails_createsNothingAndLeavesNoPairing() {
        ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9203");
        adapter.failNextReadExternalWith(5, new IllegalStateException("external system unreachable"));

        assertThatThrownBy(() -> synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user"))
                .isInstanceOf(RuntimeException.class);

        assertThat(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT))
                .noneMatch(record -> record.getExternalReference().equals(externalRef));
        assertThat(adapter.createLocalCallCount()).isZero();
    }

    @Test
    @DisplayName("the initial pass fails after creation: the entity and pairing exist without a baseline, and a later scheduled pass completes it (design.md D8)")
    void pullAndEnroll_initialPassFailsAfterCreation_leavesCreatedEntityPairedWithoutBaseline_scheduledPassCompletesIt() {
        // design.md D8: creation and pairing commit first, in their own transaction;
        // the pass that establishes the baseline runs afterwards with no transaction
        // open and commits its own outcome separately. A failure in that pass must
        // therefore leave behind exactly what a failed import leaves today: a created
        // entity, paired, with no baseline yet — not a rolled-back creation.
        adapter.withExternalState("9220", new TestSyncProjection("Sprint", "Brno-9220"));
        ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9220");
        // The first readExternal call (inside createAndPair, to build the creation
        // projection) must succeed so the entity actually gets created; only the pass's
        // own readExternal, the second call, must fail. Queued exactly 3 times — the
        // configured sync-adapter Resilience4j in-attempt retry budget (design.md D10;
        // application.yml resilience4j.retry.instances.sync-adapter.max-attempts) — so
        // the queue is fully drained by the failed pass, leaving the external system
        // healthy again for the scheduled pass that follows. RetryableSyncFailureException
        // is both a configured retry-exception (so Resilience4j actually retries
        // in-attempt) and FailureClassifier-retryable, so the pass ends in RETRYING
        // rather than FAILED.
        adapter.failReadExternalFromCallNumber(2, 3,
                new RetryableSyncFailureException("external system unreachable during initial pass"));

        SyncRecord created = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");

        // The pass's own try/catch classifies and persists the failure rather than
        // rethrowing (SynchronizationService#runPass) — pullAndEnroll returns the
        // failed record instead of throwing.
        assertThat(adapter.createLocalCallCount()).isEqualTo(1);
        assertThat(created.getStatus()).isEqualTo(SyncStatus.RETRYING);
        assertThat(created.getBaseline()).isNull();
        assertThat(created.getTarget().entityId()).isEqualTo(adapter.lastCreatedEntityId());

        // Three retryable failures alone should not trip the circuit breaker (it needs
        // a minimum of 5 calls before evaluating its failure-rate threshold), but reset
        // it defensively so this test's own induced failures cannot bleed into the
        // scheduled pass that follows.
        resetCircuitBreaker();

        // The next scheduled pass completes what the failed initial pass left behind —
        // an ordinary pass, run against the same still-healthy external state, with no
        // special-casing for "this pairing came from pullAndEnroll" (design.md D8, D3).
        SyncRecord completed = synchronizationService.runScheduledPass(created.getId());

        assertThat(completed.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(completed.getBaseline()).isNotNull();
        assertThat(completed.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno-9220"));
    }

    @Nested
    @DisplayName("an existing, in-step pairing")
    class ExistingActivePairing {

        @Test
        @DisplayName("does not create a second entity, runs an ordinary pass, and returns the existing pairing")
        void pullAndEnroll_existingInStepPairing_runsOrdinaryPassAndReturnsExistingPairing() {
            adapter.withExternalState("9210", new TestSyncProjection("Sprint", "Brno-9210"));
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9210");

            SyncRecord first = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");
            assertThat(adapter.createLocalCallCount()).isEqualTo(1);

            SyncRecord second = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");

            assertThat(second.getId()).isEqualTo(first.getId());
            assertThat(adapter.createLocalCallCount()).isEqualTo(1);
            assertThat(second.getStatus()).isEqualTo(SyncStatus.IN_SYNC);

            var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(second.getId());
            assertThat(history).hasSize(2);
        }

        /**
         * design.md D5 — the decision most worth pinning down: "pull" must not mean
         * the external system always wins. A local edit to an externally-owned field
         * (this adapter's {@code pullOnlyCreating} capability has no outward write)
         * must surface as a CONFLICT on repeat import, never be silently discarded in
         * favour of the external value.
         */
        @Test
        @DisplayName("a local edit to an externally-owned field is not overwritten on repeat import — the pairing ends in CONFLICT")
        void pullAndEnroll_localEditToExternallyOwnedField_notOverwritten_endsInConflict() {
            adapter.withExternalState("9211", new TestSyncProjection("Sprint", "Brno-9211"));
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9211");

            SyncRecord created = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");
            String entityId = created.getTarget().entityId();

            // A local edit the engine must protect: the external side is unchanged.
            adapter.withLocalState(entityId, new TestSyncProjection("Sprint (locally renamed)", "Brno-9211"));

            SyncRecord result = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");

            assertThat(result.getStatus()).isEqualTo(SyncStatus.CONFLICT);
            assertThat(result.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint (locally renamed)", "Brno-9211"));
            assertThat(adapter.createLocalCallCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("a pairing awaiting a decision (CONFLICT) is refused — nothing is written and no attempt is recorded")
        void pullAndEnroll_existingConflictPairing_refused() {
            adapter.withExternalState("9212", new TestSyncProjection("Sprint", "Brno-9212"));
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9212");

            SyncRecord created = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");
            String entityId = created.getTarget().entityId();
            adapter.withLocalState(entityId, new TestSyncProjection("Sprint (locally renamed)", "Brno-9212"));
            SyncRecord conflicted = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");
            assertThat(conflicted.getStatus()).isEqualTo(SyncStatus.CONFLICT);

            var historyBefore = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(conflicted.getId());

            assertThatThrownBy(() -> synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user"))
                    .isInstanceOf(SyncRecordNeedsResolutionException.class);

            SyncRecord unchanged = synchronizationPort.state(conflicted.getId());
            assertThat(unchanged.getStatus()).isEqualTo(SyncStatus.CONFLICT);
            assertThat(adapter.createLocalCallCount()).isEqualTo(1);
            var historyAfter = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(conflicted.getId());
            assertThat(historyAfter).hasSize(historyBefore.size());
        }

        @Test
        @DisplayName("a pairing stopped after repeated failures (FAILED) is refused — nothing is written and no attempt is recorded")
        void pullAndEnroll_existingFailedPairing_refused() {
            adapter.withExternalState("9213", new TestSyncProjection("Sprint", "Brno-9213"));
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9213");

            SyncRecord created = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");
            adapter.failNextReadExternalWith(5, new IllegalStateException("external system unreachable"));
            for (int i = 0; i < 10; i++) {
                try {
                    synchronizationPort.synchronizeNow(created.getId(), "test-user");
                } catch (RuntimeException ignored) {
                    // Driving the record to FAILED via repeated terminal/retryable failures.
                }
                if (synchronizationPort.state(created.getId()).getStatus() == SyncStatus.FAILED) {
                    break;
                }
                adapter.failNextReadExternalWith(5, new IllegalStateException("external system unreachable"));
            }
            assertThat(synchronizationPort.state(created.getId()).getStatus()).isEqualTo(SyncStatus.FAILED);

            var historyBefore = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(created.getId());

            assertThatThrownBy(() -> synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user"))
                    .isInstanceOf(SyncRecordNeedsResolutionException.class);

            SyncRecord unchanged = synchronizationPort.state(created.getId());
            assertThat(unchanged.getStatus()).isEqualTo(SyncStatus.FAILED);
            assertThat(adapter.createLocalCallCount()).isEqualTo(1);
            var historyAfter = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(created.getId());
            assertThat(historyAfter).hasSize(historyBefore.size());
        }
    }

    @Nested
    @DisplayName("an existing, retired pairing")
    class ExistingRetiredPairing {

        @Test
        @DisplayName("repeating the import returns it to service, takes the external values, and keeps earlier history")
        void pullAndEnroll_existingRetiredPairing_reactivatesAdoptsExternalAndKeepsHistory() {
            adapter.withExternalState("9214", new TestSyncProjection("Sprint", "Brno-9214"));
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9214");

            SyncRecord created = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");
            synchronizationPort.retire(created.getId());
            assertThat(synchronizationPort.state(created.getId()).getStatus()).isEqualTo(SyncStatus.RETIRED);

            var historyBeforeReactivation = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(created.getId());
            assertThat(historyBeforeReactivation).isNotEmpty();

            adapter.withExternalState("9214", new TestSyncProjection("Sprint 2", "Praha"));

            SyncRecord reactivated = synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalRef, "test-user");

            assertThat(reactivated.getId()).isEqualTo(created.getId());
            assertThat(reactivated.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            assertThat(reactivated.getRetiredAt()).isNull();
            assertThat(reactivated.getExternal().projection()).isEqualTo(new TestSyncProjection("Sprint 2", "Praha"));
            assertThat(adapter.createLocalCallCount()).isEqualTo(1);

            var historyAfterReactivation = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(reactivated.getId());
            assertThat(historyAfterReactivation.size()).isGreaterThan(historyBeforeReactivation.size());
            assertThat(historyAfterReactivation).containsAll(historyBeforeReactivation);
        }
    }
}
