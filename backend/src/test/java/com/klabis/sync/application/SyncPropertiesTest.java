package com.klabis.sync.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link SyncProperties} defaults and overrides (tasks.md 5.6, design.md D19).
 * <p>
 * Binds through {@link ApplicationContextRunner} with {@code application.yml} loaded, so the
 * effective defaults are verified without starting a full application context.
 */
@DisplayName("SyncProperties")
class SyncPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PropertiesConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SyncProperties.class)
    static class PropertiesConfiguration {
    }

    @Nested
    @DisplayName("with nothing configured")
    class Defaults {

        @Test
        @DisplayName("applies the D19 defaults")
        void appliesDefaults() {
            contextRunner.run(context -> {
                SyncProperties properties = context.getBean(SyncProperties.class);
                assertThat(properties.getMaxAttempts()).isEqualTo(5);
                assertThat(properties.getClaimLease()).isEqualTo(Duration.ofMinutes(5));
                assertThat(properties.getScanCron()).isEqualTo("0 0 2 * * *");
                assertThat(properties.getDueScanInterval()).isEqualTo(Duration.ofMinutes(15));
                assertThat(properties.getHistoryRetention()).isEqualTo(Duration.ofDays(30));
                assertThat(properties.getRetryDelay().getInitial()).isEqualTo(Duration.ofMinutes(15));
                assertThat(properties.getRetryDelay().getMultiplier()).isEqualTo(2);
                assertThat(properties.getRetryDelay().getMax()).isEqualTo(Duration.ofHours(24));
            });
        }
    }

    @Nested
    @DisplayName("with values overridden")
    class Overrides {

        @Test
        @DisplayName("takes the overridden values into account")
        void appliesOverrides() {
            contextRunner
                    .withPropertyValues(
                            "klabis.sync.scan-cron=0 30 3 * * *",
                            "klabis.sync.due-scan-interval=5m",
                            "klabis.sync.history-retention=7d")
                    .run(context -> {
                        SyncProperties properties = context.getBean(SyncProperties.class);
                        assertThat(properties.getScanCron()).isEqualTo("0 30 3 * * *");
                        assertThat(properties.getDueScanInterval()).isEqualTo(Duration.ofMinutes(5));
                        assertThat(properties.getHistoryRetention()).isEqualTo(Duration.ofDays(7));
                    });
        }
    }
}
