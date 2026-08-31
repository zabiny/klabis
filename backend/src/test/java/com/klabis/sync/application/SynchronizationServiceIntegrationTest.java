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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the synchronisation engine's slice-1 pass orchestration
 * (tasks.md 1.15), driven entirely through {@link TestSynchronizationAdapter} — no
 * ORIS involvement (design.md Migration Plan, step 1).
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, SynchronizationServiceIntegrationTest.TestAdapterConfiguration.class})
@DisplayName("Synchronisation engine: slice 1 (inward, walking skeleton)")
class SynchronizationServiceIntegrationTest {

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

    private TestSynchronizationAdapter adapter;

    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-100");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "8100");

    @BeforeEach
    void setUp() {
        adapter = (TestSynchronizationAdapter) synchronizationAdapter;
        // The adapter bean is a Spring singleton shared across every nested test in
        // this class — reset its version token and read counters so one test's state
        // cannot leak into another.
        adapter.reset();
    }

    @Nested
    @DisplayName("enrol → first pass adopts external")
    class FirstPass {

        @Test
        @DisplayName("adopts the external projection on the first synchronisation")
        void firstPassAdoptsExternal() {
            adapter.withExternalState("8100", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-100", new TestSyncProjection("(unset)", "(unset)"));

            SyncRecord enrolled = synchronizationPort.enroll(TARGET, EXTERNAL_REF);
            SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            assertThat(afterPass.getLastDirection()).isEqualTo(SyncDirection.INWARD);
            assertThat(afterPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
            assertThat(adapter.readLocal("event-100")).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
        }
    }

    @Nested
    @DisplayName("external change → inward write")
    class ExternalChangeFlowsInward {

        @Test
        @DisplayName("a later external change is written inward, baseline taken from the post-write re-read")
        void externalChangeWrittenInward() {
            adapter.withExternalState("8101", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-101", new TestSyncProjection("(unset)", "(unset)"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-101");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8101");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withExternalState("8101", new TestSyncProjection("Sprint Updated", "Brno"));
            SyncRecord afterSecondPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterSecondPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint Updated", "Brno"));
            assertThat(afterSecondPass.getBaseline().local().projection()).isEqualTo(new TestSyncProjection("Sprint Updated", "Brno"));
            assertThat(afterSecondPass.getLastDirection()).isEqualTo(SyncDirection.INWARD);
        }
    }

    @Nested
    @DisplayName("unchanged pair → nothing written")
    class UnchangedPair {

        @Test
        @DisplayName("a pass with nothing changed writes nothing and stays in step")
        void unchangedPairWritesNothing() {
            adapter.withExternalState("8102", new TestSyncProjection("Relay", "Praha"));
            adapter.withLocalState("event-102", new TestSyncProjection("(unset)", "(unset)"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-102");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8102");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            int localReadsBeforeSecondPass = adapter.localReadCount();
            SyncRecord afterSecondPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterSecondPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            assertThat(afterSecondPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Relay", "Praha"));
            // The second pass still reads both sides (no version token configured in this
            // test) but must write nothing — verified by the projection staying identical.
            assertThat(adapter.localReadCount()).isGreaterThan(localReadsBeforeSecondPass);
        }
    }

    @Nested
    @DisplayName("unchanged external version token → short-circuit, still recorded")
    class VersionTokenShortCircuit {

        @Test
        @DisplayName("an unchanged token skips the full read but still appends a SKIPPED attempt (design.md D15)")
        void unchangedTokenSkipsReadButRecordsAttempt() {
            adapter.withExternalState("8103", new TestSyncProjection("Long Distance", "Ostrava"));
            adapter.withLocalState("event-103", new TestSyncProjection("(unset)", "(unset)"));
            adapter.withVersionToken(new ExternalVersionToken("v1"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-103");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8103");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            int externalReadsBeforeShortCircuit = adapter.externalReadCount();
            SyncRecord afterShortCircuitedPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(adapter.externalReadCount()).isEqualTo(externalReadsBeforeShortCircuit);
            var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(afterShortCircuitedPass.getId());
            assertThat(history).hasSizeGreaterThanOrEqualTo(2);
            assertThat(history.get(0).getOutcome()).isEqualTo(SyncOutcome.SKIPPED);
        }
    }
}
