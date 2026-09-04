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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end tests for the synchronisation engine's pass orchestration (tasks.md 1.15,
 * 2.6, 2.7), driven entirely through {@link TestSynchronizationAdapter} — no ORIS
 * involvement (design.md Migration Plan, step 1).
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, TestAdapterConfiguration.class})
@DisplayName("Synchronisation engine: inward, outward, convergence and re-read rules")
class SynchronizationServiceIntegrationTest {

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private SynchronizationService synchronizationService;

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
        // this class — reset its version token, hooks and read counters, and default
        // capabilities to inward+outward writable, so one test's state cannot leak
        // into another. Tests that need pull-only capabilities set them explicitly.
        adapter.reset();
        adapter.withCapabilities(new SyncCapabilities(true, true, true, true, false, false, false));
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

    @Nested
    @DisplayName("local change → outward write")
    class LocalChangeFlowsOutward {

        @Test
        @DisplayName("a local change is written outward when the integration can write there")
        void localChangeWrittenOutward() {
            adapter.withExternalState("8104", new TestSyncProjection("Long Distance", "Ostrava"));
            adapter.withLocalState("event-104", new TestSyncProjection("Long Distance", "Ostrava"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-104");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8104");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-104", new TestSyncProjection("Long Distance Updated", "Ostrava"));
            SyncRecord afterSecondPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterSecondPass.getLastDirection()).isEqualTo(SyncDirection.OUTWARD);
            assertThat(afterSecondPass.getExternal().projection()).isEqualTo(new TestSyncProjection("Long Distance Updated", "Ostrava"));
            assertThat(adapter.readExternal("8104")).isEqualTo(new TestSyncProjection("Long Distance Updated", "Ostrava"));
        }
    }

    @Nested
    @DisplayName("both sides converge on the same value")
    class Convergence {

        @Test
        @DisplayName("both sides changed independently to the same value → rebase baselines, write nothing")
        void bothSidesConvergeOnSameValue() {
            adapter.withExternalState("8105", new TestSyncProjection("Middle Distance", "Zlín"));
            adapter.withLocalState("event-105", new TestSyncProjection("Middle Distance", "Zlín"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-105");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8105");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            // Both sides receive the same correction independently.
            adapter.withLocalState("event-105", new TestSyncProjection("Middle Distance Corrected", "Zlín"));
            adapter.withExternalState("8105", new TestSyncProjection("Middle Distance Corrected", "Zlín"));

            SyncRecord afterConvergedPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterConvergedPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            // A convergence is not a write: lastDirection still reflects the previous
            // pass's inward adoption, unchanged by this converged pass.
            assertThat(afterConvergedPass.getLastDirection()).isEqualTo(SyncDirection.INWARD);
            assertThat(afterConvergedPass.getBaseline().local().projection())
                    .isEqualTo(new TestSyncProjection("Middle Distance Corrected", "Zlín"));
            assertThat(afterConvergedPass.getBaseline().external().projection())
                    .isEqualTo(new TestSyncProjection("Middle Distance Corrected", "Zlín"));
        }
    }

    @Nested
    @DisplayName("concurrent local edit — inward write")
    class ConcurrentLocalEditDuringInwardWrite {

        @Test
        @DisplayName("an edit committed between the decision read and the inward write aborts the attempt")
        void localEditBetweenDecisionAndInwardWriteAborts() {
            adapter.withExternalState("8106", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-106", new TestSyncProjection("(unset)", "(unset)"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-106");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8106");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withExternalState("8106", new TestSyncProjection("Sprint Updated", "Brno"));
            adapter.resetLocalReadCount();
            // The 1st local read in this next pass is the decision read; the 2nd is the
            // guard re-read immediately before the inward write (design.md D9) — commit
            // the concurrent edit exactly there.
            adapter.onLocalReadNumber(2, () ->
                    adapter.withLocalState("event-106", new TestSyncProjection("Concurrently Edited Locally", "Brno")));

            SyncRecord afterAbortedPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(adapter.readLocal("event-106")).isEqualTo(new TestSyncProjection("Concurrently Edited Locally", "Brno"));
            assertThat(afterAbortedPass.getBaseline().local().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
            assertThat(afterAbortedPass.getLastDirection()).isEqualTo(SyncDirection.INWARD);

            var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(afterAbortedPass.getId());
            assertThat(history.get(0).getOutcome()).isEqualTo(SyncOutcome.SKIPPED);
        }
    }

    @Nested
    @DisplayName("concurrent local edit — outward write")
    class ConcurrentLocalEditDuringOutwardWrite {

        @Test
        @DisplayName("an edit committed during an outward pass rebases the baseline onto the push; the next pass pushes the newer edit again")
        void localEditDuringOutwardPassSkipsBaselineWrite() {
            adapter.withExternalState("8107", new TestSyncProjection("Relay", "Praha"));
            adapter.withLocalState("event-107", new TestSyncProjection("Relay", "Praha"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-107");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8107");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-107", new TestSyncProjection("Relay Updated", "Praha"));
            adapter.resetLocalReadCount();
            // The 1st local read in this next pass is the decision read; the 2nd is the
            // guard re-read performed right before the outward baseline write
            // (design.md D9) — commit a further concurrent edit exactly there.
            adapter.onLocalReadNumber(2, () ->
                    adapter.withLocalState("event-107", new TestSyncProjection("Relay Updated Again", "Praha")));

            SyncRecord afterSkippedAdvance = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            // The outward write to the external side still happened (it pushed what the
            // decision read saw)...
            assertThat(adapter.readExternal("8107")).isEqualTo(new TestSyncProjection("Relay Updated", "Praha"));
            // ...and the record's own external snapshot, and the WHOLE baseline pair,
            // rebase onto that push — an outward write is itself an external change the
            // engine caused (symmetric to D9's "an inward write is itself a local
            // change"), so the baseline must not stay at the old "Relay" or the next
            // pass would misread the engine's own write as an independent external
            // change. This must be a normal reconciled (equal-halves) baseline, not the
            // divergent shape D6 reserves for an accepted divergence.
            assertThat(afterSkippedAdvance.getExternal().projection()).isEqualTo(new TestSyncProjection("Relay Updated", "Praha"));
            assertThat(afterSkippedAdvance.getBaseline().isDiverged()).isFalse();
            assertThat(afterSkippedAdvance.getBaseline().local().projection()).isEqualTo(new TestSyncProjection("Relay Updated", "Praha"));
            assertThat(afterSkippedAdvance.getBaseline().external().projection()).isEqualTo(new TestSyncProjection("Relay Updated", "Praha"));

            var history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(afterSkippedAdvance.getId());
            assertThat(history.get(0).getOutcome()).isEqualTo(SyncOutcome.SKIPPED);

            // The next pass re-evaluates: local is "Relay Updated Again" (changed since
            // the rebased baseline "Relay Updated"), external is "Relay Updated"
            // (unchanged since that same baseline) — a clean outward push of the newest
            // local edit, never a false conflict with the engine's own prior write.
            adapter.resetLocalReadCount();
            SyncRecord afterFollowUpPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterFollowUpPass.getLastDirection()).isEqualTo(SyncDirection.OUTWARD);
            assertThat(afterFollowUpPass.getExternal().projection()).isEqualTo(new TestSyncProjection("Relay Updated Again", "Praha"));
            assertThat(adapter.readExternal("8107")).isEqualTo(new TestSyncProjection("Relay Updated Again", "Praha"));
        }
    }

    @Nested
    @DisplayName("an inward write is itself a local change, but raises no conflict")
    class InwardWriteIsItselfALocalChange {

        @Test
        @DisplayName("the pass after an inward write lands on nothing-to-do, never a conflict (design.md D9)")
        void passAfterInwardWriteResolvesWithoutConflict() {
            adapter.withExternalState("8108", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-108", new TestSyncProjection("(unset)", "(unset)"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-108");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8108");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            SyncRecord afterInwardWrite = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            assertThat(afterInwardWrite.getLastDirection()).isEqualTo(SyncDirection.INWARD);

            // In the real system, Event.syncFromOris publishes EventUpdatedEvent exactly
            // like every other mutating command, so the inward write above would mark
            // this very record dirty (design.md D9). That trigger wiring is added by a
            // later slice (events module integration); here we drive the pass the same
            // trigger would cause and assert it resolves to "nothing to do" rather than
            // misreading the write it just performed as a conflicting local edit.
            SyncRecord afterFollowUpPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterFollowUpPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            assertThat(afterFollowUpPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
        }
    }

    @Nested
    @DisplayName("conflicts: detection")
    class ConflictDetection {

        @Test
        @DisplayName("both sides changed to different values → conflict, neither side written")
        void bothSidesChangedDifferently_conflict() {
            adapter.withExternalState("8109", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-109", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-109");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8109");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-109", new TestSyncProjection("Sprint Local Edit", "Brno"));
            adapter.withExternalState("8109", new TestSyncProjection("Sprint External Edit", "Brno"));

            SyncRecord afterConflictPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterConflictPass.getStatus()).isEqualTo(SyncStatus.CONFLICT);
            assertThat(afterConflictPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint Local Edit", "Brno"));
            assertThat(afterConflictPass.getExternal().projection()).isEqualTo(new TestSyncProjection("Sprint External Edit", "Brno"));
            // Neither side was written.
            assertThat(adapter.readLocal("event-109")).isEqualTo(new TestSyncProjection("Sprint Local Edit", "Brno"));
            assertThat(adapter.readExternal("8109")).isEqualTo(new TestSyncProjection("Sprint External Edit", "Brno"));
        }

        @Test
        @DisplayName("a local change with no outward write capability → conflict, the local edit survives (design.md D6)")
        void localChangeWithNoOutwardCapability_conflict() {
            adapter.withCapabilities(new SyncCapabilities(true, true, true, false, false, false, false));
            adapter.withExternalState("8110", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-110", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-110");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8110");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-110", new TestSyncProjection("Manager's Correction", "Brno"));
            SyncRecord afterConflictPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThat(afterConflictPass.getStatus()).isEqualTo(SyncStatus.CONFLICT);
            assertThat(afterConflictPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Manager's Correction", "Brno"));
            // The edit was never overwritten.
            assertThat(adapter.readLocal("event-110")).isEqualTo(new TestSyncProjection("Manager's Correction", "Brno"));
        }

        @Test
        @DisplayName("a conflict clears itself when a side reverts")
        void conflictClearsWhenSideReverts() {
            adapter.withExternalState("8111", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-111", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-111");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8111");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-111", new TestSyncProjection("Sprint Local Edit", "Brno"));
            adapter.withExternalState("8111", new TestSyncProjection("Sprint External Edit", "Brno"));
            SyncRecord conflicted = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            assertThat(conflicted.getStatus()).isEqualTo(SyncStatus.CONFLICT);

            // The external side reverts to the agreed baseline value. A conflict is
            // re-evaluated on every pass and can clear itself (design.md D7) — that is
            // the scheduled cadence's job, not the manual trigger's (synchronizeNow
            // refuses a CONFLICT record outright), so this drives the pass directly.
            adapter.withExternalState("8111", new TestSyncProjection("Sprint", "Brno"));
            SyncRecord afterRevert = synchronizationService.runScheduledPass(enrolled.getId());

            assertThat(afterRevert.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            assertThat(afterRevert.getLastDirection()).isEqualTo(SyncDirection.OUTWARD);
        }

        @Test
        @DisplayName("a conflict clears itself when both sides come to agree (fixed in the external system)")
        void conflictClearsWhenBothSidesAgree() {
            adapter.withExternalState("8112", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-112", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-112");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8112");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-112", new TestSyncProjection("Sprint Corrected", "Brno"));
            adapter.withExternalState("8112", new TestSyncProjection("Sprint External Edit", "Brno"));
            SyncRecord conflicted = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
            assertThat(conflicted.getStatus()).isEqualTo(SyncStatus.CONFLICT);

            // The manager corrects the external system directly to match Klabis (D6
            // exit 2) — no engine operation involved. The record clears its own
            // conflict on the next pass (design.md D7), driven here as a scheduled
            // pass since synchronizeNow refuses a CONFLICT record outright.
            adapter.withExternalState("8112", new TestSyncProjection("Sprint Corrected", "Brno"));
            SyncRecord afterFix = synchronizationService.runScheduledPass(enrolled.getId());

            assertThat(afterFix.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        }

        @Test
        @DisplayName("a conflicted record is written to by no pass")
        void conflictedRecordWrittenToByNoPass() {
            adapter.withExternalState("8113", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-113", new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-113");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8113");

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState("event-113", new TestSyncProjection("Sprint Local Edit", "Brno"));
            adapter.withExternalState("8113", new TestSyncProjection("Sprint External Edit", "Brno"));
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            // A further pass while the conflict stands must still write nothing —
            // driven as a scheduled pass, since a manual synchronizeNow now refuses a
            // CONFLICT record outright rather than silently doing nothing.
            synchronizationService.runScheduledPass(enrolled.getId());

            assertThat(adapter.readLocal("event-113")).isEqualTo(new TestSyncProjection("Sprint Local Edit", "Brno"));
            assertThat(adapter.readExternal("8113")).isEqualTo(new TestSyncProjection("Sprint External Edit", "Brno"));
        }
    }

    @Nested
    @DisplayName("conflicts: resolution")
    class ConflictResolution {

        @Test
        @DisplayName("resolving without acknowledging is refused")
        void resolvingWithoutAcknowledgingIsRefused() {
            SyncRecord enrolled = setUpConflictedRecord("event-114", "8114");

            assertThatThrownBy(() -> synchronizationPort.resolveConflict(enrolled.getId(), SyncResolution.ACCEPT_DIVERGENCE, "manager"))
                    .isInstanceOf(ConflictNotAcknowledgedException.class);
        }

        @Test
        @DisplayName("acknowledging a conflict that no longer exists is refused")
        void acknowledgingNonexistentConflictIsRefused() {
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-115");
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "8115");
            adapter.withExternalState("8115", new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState("event-115", new TestSyncProjection("Sprint", "Brno"));
            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            assertThatThrownBy(() -> synchronizationPort.acknowledgeConflict(enrolled.getId(), "manager"))
                    .isInstanceOf(SyncRecordNotInConflictException.class);
        }

        @Test
        @DisplayName("forcing INWARD discards the local edit and pulls the external value")
        void forceInwardDiscardsLocalEdit() {
            SyncRecord conflicted = setUpConflictedRecord("event-116", "8116");
            synchronizationPort.acknowledgeConflict(conflicted.getId(), "manager");

            SyncRecord resolved = synchronizationPort.resolveConflict(conflicted.getId(), SyncResolution.INWARD, "manager");

            assertThat(resolved.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            assertThat(resolved.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint External Edit", "Brno"));
            assertThat(adapter.readLocal("event-116")).isEqualTo(new TestSyncProjection("Sprint External Edit", "Brno"));
        }

        @Test
        @DisplayName("ACCEPT_DIVERGENCE writes nothing, sets a diverged baseline, and clears the conflict")
        void acceptDivergenceWritesNothingAndClearsConflict() {
            SyncRecord conflicted = setUpConflictedRecord("event-117", "8117");
            synchronizationPort.acknowledgeConflict(conflicted.getId(), "manager");

            SyncRecord resolved = synchronizationPort.resolveConflict(conflicted.getId(), SyncResolution.ACCEPT_DIVERGENCE, "manager");

            assertThat(resolved.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
            assertThat(resolved.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint Local Edit", "Brno"));
            assertThat(resolved.getExternal().projection()).isEqualTo(new TestSyncProjection("Sprint External Edit", "Brno"));
            assertThat(resolved.getBaseline().isDiverged()).isTrue();
            // Nothing was written on either side.
            assertThat(adapter.readLocal("event-117")).isEqualTo(new TestSyncProjection("Sprint Local Edit", "Brno"));
            assertThat(adapter.readExternal("8117")).isEqualTo(new TestSyncProjection("Sprint External Edit", "Brno"));
        }

        @Test
        @DisplayName("after an accepted divergence, a later external change raises a new conflict instead of overwriting")
        void acceptedDivergenceProtectedFromLaterExternalChange() {
            SyncRecord conflicted = setUpConflictedRecord("event-118", "8118");
            synchronizationPort.acknowledgeConflict(conflicted.getId(), "manager");
            synchronizationPort.resolveConflict(conflicted.getId(), SyncResolution.ACCEPT_DIVERGENCE, "manager");

            adapter.withExternalState("8118", new TestSyncProjection("Sprint Yet Another External Change", "Brno"));
            SyncRecord afterLaterExternalChange = synchronizationPort.synchronizeNow(conflicted.getId(), "test-user");

            assertThat(afterLaterExternalChange.getStatus()).isEqualTo(SyncStatus.CONFLICT);
            // The accepted local value was never overwritten.
            assertThat(afterLaterExternalChange.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint Local Edit", "Brno"));
            assertThat(adapter.readLocal("event-118")).isEqualTo(new TestSyncProjection("Sprint Local Edit", "Brno"));
        }

        @Test
        @DisplayName("OUTWARD is refused for an integration that cannot write there")
        void outwardRefusedWhenUnsupported() {
            adapter.withCapabilities(new SyncCapabilities(true, true, true, false, false, false, false));
            SyncRecord conflicted = setUpConflictedRecordWithLocalOnlyChange("event-119", "8119");
            synchronizationPort.acknowledgeConflict(conflicted.getId(), "manager");

            assertThatThrownBy(() -> synchronizationPort.resolveConflict(conflicted.getId(), SyncResolution.OUTWARD, "manager"))
                    .isInstanceOf(UnsupportedResolutionException.class);
        }

        @Test
        @DisplayName("a side moving after acknowledgement refuses the resolution, refreshes snapshots, leaves the conflict standing")
        void sideMovingAfterAcknowledgementRefusesResolution() {
            SyncRecord conflicted = setUpConflictedRecord("event-120", "8120");
            synchronizationPort.acknowledgeConflict(conflicted.getId(), "manager");

            // The external side moves again after acknowledgement but before resolution.
            adapter.withExternalState("8120", new TestSyncProjection("Sprint Yet Another External Change", "Brno"));

            assertThatThrownBy(() -> synchronizationPort.resolveConflict(conflicted.getId(), SyncResolution.INWARD, "manager"))
                    .isInstanceOf(ConflictNotAcknowledgedException.class);

            SyncRecord afterRefusedResolution = synchronizationPort.state(conflicted.getId());
            assertThat(afterRefusedResolution.getStatus()).isEqualTo(SyncStatus.CONFLICT);
            // The record's snapshots were refreshed to the new collision.
            assertThat(afterRefusedResolution.getExternal().projection())
                    .isEqualTo(new TestSyncProjection("Sprint Yet Another External Change", "Brno"));
        }

        private SyncRecord setUpConflictedRecord(String entityId, String externalId) {
            adapter.withExternalState(externalId, new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState(entityId, new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, entityId);
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, externalId);

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState(entityId, new TestSyncProjection("Sprint Local Edit", "Brno"));
            adapter.withExternalState(externalId, new TestSyncProjection("Sprint External Edit", "Brno"));
            return synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
        }

        private SyncRecord setUpConflictedRecordWithLocalOnlyChange(String entityId, String externalId) {
            adapter.withExternalState(externalId, new TestSyncProjection("Sprint", "Brno"));
            adapter.withLocalState(entityId, new TestSyncProjection("Sprint", "Brno"));
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, entityId);
            ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, externalId);

            SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
            synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

            adapter.withLocalState(entityId, new TestSyncProjection("Sprint Local Only Edit", "Brno"));
            return synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");
        }
    }
}
