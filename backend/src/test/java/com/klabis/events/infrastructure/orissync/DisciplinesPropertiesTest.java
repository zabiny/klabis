package com.klabis.events.infrastructure.orissync;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link DisciplinesProperties} defaults/overrides and confirms
 * {@link DisciplineDiscoveryJob#discoverNewDisciplines()} is scheduled off the same
 * property key (design.md D4, tasks.md 5.2) — independent of
 * {@code com.klabis.sync.application.SyncProperties}'s own {@code scan-cron}/
 * {@code due-scan-interval}.
 * <p>
 * Uses {@code @SpringBootTest} rather than {@code @ApplicationModuleTest(STANDALONE)}
 * (unlike {@code SyncPropertiesTest}'s otherwise-identical shape): {@code
 * DisciplinesProperties} lives in {@code events.infrastructure.orissync}, a nested
 * module boundary within {@code events}, and a STANDALONE bootstrap scoped to that
 * nested module does not pick up {@code @ConfigurationPropertiesScan}'s registration
 * from {@code KlabisApplication} — confirmed by a real CI run
 * ({@code NoSuchBeanDefinitionException} for exactly this bean). A full
 * {@code @SpringBootTest} context (as {@link DisciplineDiscoverySyncScenarioIntegrationTest}
 * already uses successfully in this same package) does not have that limitation.
 */
@DisplayName("DisciplinesProperties")
class DisciplinesPropertiesTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @CleanupTestData
    @Import(TestApplicationConfiguration.class)
    @DisplayName("with nothing configured")
    class Defaults {

        @Autowired
        private DisciplinesProperties properties;

        @Test
        @DisplayName("applies the D4 default: nightly, offset from klabis.sync.scan-cron")
        void appliesDefault() {
            assertThat(properties.getDiscoveryCron()).isEqualTo("0 0 3 * * *");
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @CleanupTestData
    @Import(TestApplicationConfiguration.class)
    @DisplayName("with values overridden")
    class Overrides {

        @DynamicPropertySource
        static void overrideProperties(DynamicPropertyRegistry registry) {
            registry.add("klabis.disciplines.discovery-cron", () -> "0 15 4 * * *");
        }

        @Autowired
        private DisciplinesProperties properties;

        @Test
        @DisplayName("takes the overridden value into account")
        void appliesOverride() {
            assertThat(properties.getDiscoveryCron()).isEqualTo("0 15 4 * * *");
        }
    }

    @Nested
    @DisplayName("DisciplineDiscoveryJob scheduling wiring")
    class ScheduleWiring {

        @Test
        @DisplayName("discoverNewDisciplines is scheduled off klabis.disciplines.discovery-cron, not klabis.sync's cron properties")
        void scheduledOffOwnCronProperty() throws NoSuchMethodException {
            Scheduled scheduled = DisciplineDiscoveryJob.class
                    .getDeclaredMethod("discoverNewDisciplines")
                    .getAnnotation(Scheduled.class);

            assertThat(scheduled).isNotNull();
            assertThat(scheduled.cron()).isEqualTo("${klabis.disciplines.discovery-cron}");
        }
    }
}
