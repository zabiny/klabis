package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.domain.*;
import com.klabis.sync.fixtures.TestAdapterConfiguration;
import com.klabis.sync.fixtures.TestSyncProjection;
import com.klabis.sync.fixtures.TestSynchronizationAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SynchronizationPort#markDirty} (task 8.2, design.md D9): a consuming module
 * marks a record due after observing a local change, without running a pass inline.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, TestAdapterConfiguration.class})
@DisplayName("SynchronizationPort#markDirty")
class SynchronizationServiceMarkDirtyIntegrationTest {

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private SynchronizationAdapter synchronizationAdapter;

    private TestSynchronizationAdapter adapter;

    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-dirty-1");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "9100");

    @BeforeEach
    void setUp() {
        adapter = (TestSynchronizationAdapter) synchronizationAdapter;
        adapter.reset();
        adapter.withCapabilities(SyncCapabilities.bidirectional());
    }

    @Test
    @DisplayName("sets dirtySince on an enrolled record")
    void marksEnrolledRecordDirty() {
        adapter.withExternalState("9100", new TestSyncProjection("Sprint", "Brno"));
        adapter.withLocalState("event-dirty-1", new TestSyncProjection("Sprint", "Brno"));
        SyncRecord enrolled = synchronizationPort.enroll(TARGET, EXTERNAL_REF);
        synchronizationPort.synchronizeNow(enrolled.getId(), null);

        synchronizationPort.markDirty(TARGET);

        SyncRecord record = synchronizationPort.state(enrolled.getId());
        assertThat(record.getDirtySince()).isNotNull();
    }

    @Test
    @DisplayName("does nothing when the target is not enrolled")
    void doesNothingWhenNotEnrolled() {
        SyncTarget unenrolled = new SyncTarget(SyncEntityType.EVENT, "event-never-enrolled");

        synchronizationPort.markDirty(unenrolled);

        assertThat(synchronizationPort.findByTarget(unenrolled)).isEmpty();
    }

    /**
     * Pins the same interleaving that used to reproduce the {@code markDirty} /
     * {@code SyncOutcomeWriter#persist} version collision the change's proposal.md
     * describes (proposal.md task 1.2, task 6.1): an inward pass reads the post-write
     * local state, and — before {@code SyncOutcomeWriter#persist} saves the record — a
     * concurrent {@code markDirty} call runs to completion first. What this now proves
     * is that the race is structurally impossible rather than "won": {@code markDirty}
     * (task 4.3) writes only through {@code SyncScheduleRepository} against the
     * unversioned {@code sync_schedule} table (task 2.3) and never loads or saves the
     * {@code sync_record} row at all, so there is no longer a shared version for the
     * two writes to contend over. This test is the proof for task 6.1 — it stays even
     * though it can no longer fail for the old reason, because it still exercises the
     * exact interleaving that used to matter.
     * <p>
     * {@link TestSynchronizationAdapter#onLocalReadNumber} pins the interleaving to
     * the point the race used to occur — the post-write re-read inside
     * {@code writeInward}, immediately before the record is handed to
     * {@code outcomeWriter.persist} — rather than hoping an asynchronous listener
     * happens to lose the race within a test timeout.
     */
    @Test
    @DisplayName("a concurrent markDirty on the same record cannot disrupt the pass's own save")
    void passSaveSurvivesConcurrentMarkDirty() throws Exception {
        SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-race-1");
        ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9200");
        adapter.withExternalState("9200", new TestSyncProjection("Sprint", "Brno"));
        adapter.withLocalState("event-race-1", new TestSyncProjection("Sprint", "Brno"));
        SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
        // First pass: NEW -> IN_SYNC, adopting the external side (design.md D5). Baseline
        // now exists, so the second pass below takes the ordinary inward-write path.
        synchronizationPort.synchronizeNow(enrolled.getId(), null);

        // The external side changes; the next pass will write inward and, at the very
        // end, race a concurrent markDirty on the same row.
        adapter.withExternalState("9200", new TestSyncProjection("Sprint", "Praha"));

        CountDownLatch markDirtySaved = new CountDownLatch(1);
        adapter.resetLocalReadCount();
        adapter.onLocalReadNumber(3, () -> {
            // This is the post-write re-read inside writeInward (decision read = 1,
            // writeInward's guard re-read = 2, post-write re-read = 3) — the exact
            // point SyncOutcomeWriter's javadoc names: the pass is about to persist a
            // record it read/holds a version for, and a concurrent markDirty on the
            // same row must complete its own load-mutate-save first for the race to be
            // genuine.
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<?> future = executor.submit(() -> synchronizationPort.markDirty(target));
                future.get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
            markDirtySaved.countDown();
        });

        // Must not throw: markDirty writes sync_schedule alone (no version on that
        // table, task 2.3), so it cannot collide with sync_record's version at all —
        // there is no lock left to lose a race over.
        SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), null);

        assertThat(markDirtySaved.getCount()).isZero();
        assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(afterPass.getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Praha"));

        SyncRecord persisted = synchronizationPort.state(enrolled.getId());
        assertThat(persisted.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
    }
}
