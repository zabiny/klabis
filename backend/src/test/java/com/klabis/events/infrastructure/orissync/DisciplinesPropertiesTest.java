package com.klabis.events.infrastructure.orissync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link DisciplinesProperties} defaults/overrides and confirms
 * {@link DisciplineDiscoveryJob#discoverNewDisciplines()} is scheduled off the same
 * property key (design.md D4, tasks.md 5.2) — independent of
 * {@code com.klabis.sync.application.SyncProperties}'s own {@code scan-cron}/
 * {@code due-scan-interval}.
 * <p>
 * Binds through {@link ApplicationContextRunner} with {@code application.yml} loaded, so the
 * effective default (Java field default combined with the yml placeholder default) is verified
 * without starting a full application context.
 */
@DisplayName("DisciplinesProperties")
class DisciplinesPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PropertiesConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DisciplinesProperties.class)
    static class PropertiesConfiguration {
    }

    @Nested
    @DisplayName("with nothing configured")
    class Defaults {

        @Test
        @DisplayName("applies the D4 default: nightly, offset from klabis.sync.scan-cron")
        void appliesDefault() {
            contextRunner.run(context ->
                    assertThat(context.getBean(DisciplinesProperties.class).getDiscoveryCron()).isEqualTo("0 0 3 * * *"));
        }
    }

    @Nested
    @DisplayName("with values overridden")
    class Overrides {

        @Test
        @DisplayName("takes the overridden value into account")
        void appliesOverride() {
            contextRunner
                    .withPropertyValues("klabis.disciplines.discovery-cron=0 15 4 * * *")
                    .run(context ->
                            assertThat(context.getBean(DisciplinesProperties.class).getDiscoveryCron()).isEqualTo("0 15 4 * * *"));
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
