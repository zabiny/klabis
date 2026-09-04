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
        adapter.withCapabilities(new SyncCapabilities(true, true, true, true, false, false, false));
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
}
