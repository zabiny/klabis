package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Enrolment with no {@code SynchronizationAdapter} bean registered at all — the
 * unknown-entity-type rejection path (tasks.md 1.15). Deliberately a separate context
 * from {@link SynchronizationServiceIntegrationTest}, which registers a test adapter.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import(TestApplicationConfiguration.class)
@DisplayName("Synchronisation engine: enrolment with no registered adapter")
class SynchronizationServiceNoAdapterIntegrationTest {

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Test
    @DisplayName("enrolment is rejected when no adapter is registered for the entity type and external system")
    void enrolmentRejectedWithNoAdapter() {
        SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-1");
        ExternalReference externalReference = new ExternalReference(ExternalSystem.ORIS, "8100");

        assertThatThrownBy(() -> synchronizationPort.enroll(target, externalReference))
                .isInstanceOf(UnknownSyncEntityTypeException.class);
    }
}
