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
    private SynchronizationAdapter synchronizationAdapter;

    @Autowired
    private SyncAttemptRepository syncAttemptRepository;

    private TestSynchronizationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = (TestSynchronizationAdapter) synchronizationAdapter;
        adapter.reset();
        adapter.withCapabilities(SyncCapabilities.pullOnlyCreating());
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
}
