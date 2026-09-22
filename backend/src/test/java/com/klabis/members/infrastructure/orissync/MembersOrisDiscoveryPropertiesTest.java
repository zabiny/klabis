package com.klabis.members.infrastructure.orissync;

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
 * Tests {@link MembersOrisDiscoveryProperties} defaults/overrides and confirms
 * {@link MemberDiscoveryJob#discoverNewMembers()} is scheduled off its own property
 * key (design.md D7, tasks.md 8.5) — independent of both
 * {@code com.klabis.sync.application.SyncProperties}'s {@code scan-cron}/
 * {@code due-scan-interval} and {@code events.infrastructure.orissync.DisciplinesProperties}'s
 * {@code discovery-cron}.
 * <p>
 * Mirrors {@code DisciplinesPropertiesTest}'s shape: {@code @SpringBootTest} rather
 * than {@code @ApplicationModuleTest(STANDALONE)}, since a STANDALONE bootstrap scoped
 * to the nested {@code members.infrastructure.orissync} module boundary does not pick
 * up {@code @ConfigurationPropertiesScan}'s registration from {@code KlabisApplication}.
 */
@DisplayName("MembersOrisDiscoveryProperties")
class MembersOrisDiscoveryPropertiesTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @CleanupTestData
    @Import(TestApplicationConfiguration.class)
    @DisplayName("with nothing configured")
    class Defaults {

        @Autowired
        private MembersOrisDiscoveryProperties properties;

        @Test
        @DisplayName("applies the D7 default")
        void appliesDefault() {
            assertThat(properties.getOrisDiscoveryCron()).isEqualTo("0 30 3 * * *");
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
            registry.add("klabis.members.oris-discovery-cron", () -> "0 45 5 * * *");
        }

        @Autowired
        private MembersOrisDiscoveryProperties properties;

        @Test
        @DisplayName("takes the overridden value into account")
        void appliesOverride() {
            assertThat(properties.getOrisDiscoveryCron()).isEqualTo("0 45 5 * * *");
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
