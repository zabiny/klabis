package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * design.md D1: {@link SynchronizationPort#pullAndEnroll} with no
 * {@code SynchronizationAdapter} bean registered at all for the given entity type and
 * external system — mirrors {@link SynchronizationServiceNoAdapterIntegrationTest}'s
 * separate, adapter-free context.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import(TestApplicationConfiguration.class)
@DisplayName("pullAndEnroll: no registered adapter")
class SynchronizationServicePullAndEnrollNoAdapterIntegrationTest {

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Test
    @DisplayName("is refused with a clear exception when no adapter is registered for the entity type and external system")
    void pullAndEnroll_noAdapterRegistered_refused() {
        ExternalReference externalReference = new ExternalReference(ExternalSystem.ORIS, "9300");

        assertThatThrownBy(() -> synchronizationPort.pullAndEnroll(SyncEntityType.EVENT, externalReference, "test-user"))
                .isInstanceOf(UnknownSyncEntityTypeException.class);
    }
}
