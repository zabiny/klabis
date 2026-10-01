package com.klabis.members.infrastructure.orissync;

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
 * Tests {@link MembersOrisDiscoveryProperties} defaults/overrides and confirms
 * {@link MemberDiscoveryJob#discoverNewMembers()} is scheduled off its own property
 * key (design.md D7, tasks.md 8.5) — independent of both
 * {@code com.klabis.sync.application.SyncProperties}'s {@code scan-cron}/
 * {@code due-scan-interval} and {@code events.infrastructure.orissync.DisciplinesProperties}'s
 * {@code discovery-cron}.
 * <p>
 * Binds through {@link ApplicationContextRunner} with {@code application.yml} loaded, so the
 * effective default is verified without starting a full application context.
 */
@DisplayName("MembersOrisDiscoveryProperties")
class MembersOrisDiscoveryPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PropertiesConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MembersOrisDiscoveryProperties.class)
    static class PropertiesConfiguration {
    }

    @Nested
    @DisplayName("with nothing configured")
    class Defaults {

        @Test
        @DisplayName("applies the D7 default")
        void appliesDefault() {
            contextRunner.run(context ->
                    assertThat(context.getBean(MembersOrisDiscoveryProperties.class).getOrisDiscoveryCron()).isEqualTo("0 30 3 * * *"));
        }
    }

    @Nested
    @DisplayName("with values overridden")
    class Overrides {

        @Test
        @DisplayName("takes the overridden value into account")
        void appliesOverride() {
            contextRunner
                    .withPropertyValues("klabis.members.oris-discovery-cron=0 45 5 * * *")
                    .run(context ->
                            assertThat(context.getBean(MembersOrisDiscoveryProperties.class).getOrisDiscoveryCron()).isEqualTo("0 45 5 * * *"));
        }
    }

    @Nested
    @DisplayName("MemberDiscoveryJob scheduling wiring")
    class ScheduleWiring {

        @Test
        @DisplayName("discoverNewMembers is scheduled off klabis.members.oris-discovery-cron, not klabis.sync's or klabis.disciplines' cron properties")
        void scheduledOffOwnCronProperty() throws NoSuchMethodException {
            Scheduled scheduled = MemberDiscoveryJob.class
                    .getDeclaredMethod("discoverNewMembers")
                    .getAnnotation(Scheduled.class);

            assertThat(scheduled).isNotNull();
            assertThat(scheduled.cron()).isEqualTo("${klabis.members.oris-discovery-cron}");
        }
    }
}
