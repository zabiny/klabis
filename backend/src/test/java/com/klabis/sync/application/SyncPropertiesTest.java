package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link SyncProperties} defaults and overrides (tasks.md 5.6, design.md D19).
 */
@DisplayName("SyncProperties")
class SyncPropertiesTest {

    @Nested
    @ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
    @ActiveProfiles("test")
    @CleanupTestData
    @Import(TestApplicationConfiguration.class)
    @DisplayName("with nothing configured")
    class Defaults {

        @Autowired
        private SyncProperties properties;

        @Test
        @DisplayName("applies the D19 defaults")
        void appliesDefaults() {
            assertThat(properties.getMaxAttempts()).isEqualTo(5);
            assertThat(properties.getClaimLease()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.getScanCron()).isEqualTo("0 0 2 * * *");
            assertThat(properties.getDueScanInterval()).isEqualTo(Duration.ofMinutes(15));
            assertThat(properties.getHistoryRetention()).isEqualTo(Duration.ofDays(30));
            assertThat(properties.getRetryDelay().getInitial()).isEqualTo(Duration.ofMinutes(15));
            assertThat(properties.getRetryDelay().getMultiplier()).isEqualTo(2);
            assertThat(properties.getRetryDelay().getMax()).isEqualTo(Duration.ofHours(24));
        }
    }

    @Nested
    @ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
    @ActiveProfiles("test")
    @CleanupTestData
    @Import(TestApplicationConfiguration.class)
    @DisplayName("with values overridden")
    class Overrides {

        @DynamicPropertySource
        static void overrideProperties(DynamicPropertyRegistry registry) {
            registry.add("klabis.sync.scan-cron", () -> "0 30 3 * * *");
            registry.add("klabis.sync.due-scan-interval", () -> "5m");
            registry.add("klabis.sync.history-retention", () -> "7d");
        }

        @Autowired
        private SyncProperties properties;

        @Test
        @DisplayName("takes the overridden values into account")
        void appliesOverrides() {
            assertThat(properties.getScanCron()).isEqualTo("0 30 3 * * *");
            assertThat(properties.getDueScanInterval()).isEqualTo(Duration.ofMinutes(5));
            assertThat(properties.getHistoryRetention()).isEqualTo(Duration.ofDays(7));
        }
    }
}
