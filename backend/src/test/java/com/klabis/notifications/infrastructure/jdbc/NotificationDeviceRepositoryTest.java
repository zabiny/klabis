package com.klabis.notifications.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.common.users.UserId;
import com.klabis.notifications.NotificationDeviceTestDataBuilder;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.NotificationDeviceFilter;
import com.klabis.notifications.domain.NotificationDeviceRepository;
import com.klabis.notifications.domain.PushEndpoint;
import com.klabis.notifications.domain.PushKeys;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificationDevice Persistence")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO common.users (id, user_name, password_hash, account_status, created_at, modified_at, version) VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ZBM0001', 'hash', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
        "INSERT INTO common.users (id, user_name, password_hash, account_status, created_at, modified_at, version) VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ZBM0002', 'hash', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)"
})
class NotificationDeviceRepositoryTest {

    private static final UserId USER_A = new UserId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final UserId USER_B = new UserId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));

    @Autowired
    private NotificationDeviceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("round-trips the device with audit metadata")
        void roundTrip() {
            NotificationDevice device = NotificationDeviceTestDataBuilder.aDevice()
                    .withOwner(USER_A)
                    .withEndpoint("https://push.example.org/send/1")
                    .withKeys(new PushKeys("p256dh-value", "auth-value"))
                    .withDescription("Chrome · Android")
                    .build();

            NotificationDevice saved = repository.save(device);
            NotificationDevice loaded = repository.findOne(
                    NotificationDeviceFilter.all().withEndpoint(new PushEndpoint("https://push.example.org/send/1")))
                    .orElseThrow();

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(loaded.getId()).isEqualTo(device.getId());
            assertThat(loaded.getOwner()).isEqualTo(USER_A);
            assertThat(loaded.getKeys()).isEqualTo(new PushKeys("p256dh-value", "auth-value"));
            assertThat(loaded.getDescription()).isEqualTo("Chrome · Android");
            assertThat(loaded.getEnabledAt()).isCloseTo(device.getEnabledAt(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MILLIS));
        }

        @Test
        @DisplayName("stores the keys encrypted")
        void keysAreEncrypted() {
            repository.save(NotificationDeviceTestDataBuilder.aDevice()
                    .withOwner(USER_A)
                    .withKeys(new PushKeys("p256dh-plain", "auth-plain"))
                    .build());

            String storedP256dh = jdbcTemplate.queryForObject(
                    "SELECT p256dh FROM notifications.notification_device", String.class);
            String storedAuth = jdbcTemplate.queryForObject(
                    "SELECT auth FROM notifications.notification_device", String.class);

            assertThat(storedP256dh).isNotEqualTo("p256dh-plain");
            assertThat(storedAuth).isNotEqualTo("auth-plain");
        }

        @Test
        @DisplayName("persists a reassignment of an existing device")
        void persistsReassignment() {
            NotificationDevice saved = repository.save(
                    NotificationDeviceTestDataBuilder.aDevice().withOwner(USER_A).build());

            NotificationDevice loaded = repository.findAll(NotificationDeviceFilter.all().withOwner(USER_A)).getFirst();
            loaded.reassignTo(USER_B);
            repository.save(loaded);

            assertThat(repository.findAll(NotificationDeviceFilter.all().withOwner(USER_A))).isEmpty();
            assertThat(repository.findAll(NotificationDeviceFilter.all().withOwner(USER_B)))
                    .extracting(NotificationDevice::getId)
                    .containsExactly(saved.getId());
        }

        @Test
        @DisplayName("rejects a second device with the same endpoint")
        void endpointIsUnique() {
            repository.save(NotificationDeviceTestDataBuilder.aDevice()
                    .withOwner(USER_A).withEndpoint("https://push.example.org/same").build());

            assertThatThrownBy(() -> repository.save(NotificationDeviceTestDataBuilder.aDevice()
                    .withOwner(USER_B).withEndpoint("https://push.example.org/same").build()))
                    .isInstanceOf(DuplicateKeyException.class);
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("returns only devices of the given owner")
        void filtersByOwner() {
            NotificationDevice a1 = repository.save(NotificationDeviceTestDataBuilder.aDevice().withOwner(USER_A).build());
            NotificationDevice a2 = repository.save(NotificationDeviceTestDataBuilder.aDevice().withOwner(USER_A).build());
            repository.save(NotificationDeviceTestDataBuilder.aDevice().withOwner(USER_B).build());

            List<NotificationDevice> found = repository.findAll(NotificationDeviceFilter.all().withOwner(USER_A));

            assertThat(found).extracting(NotificationDevice::getId).containsExactlyInAnyOrder(a1.getId(), a2.getId());
        }

        @Test
        @DisplayName("returns all devices for an empty filter")
        void emptyFilterReturnsAll() {
            repository.save(NotificationDeviceTestDataBuilder.aDevice().withOwner(USER_A).build());
            repository.save(NotificationDeviceTestDataBuilder.aDevice().withOwner(USER_B).build());

            assertThat(repository.findAll(NotificationDeviceFilter.all())).hasSize(2);
        }

        @Test
        @DisplayName("deleting the user cascades to the devices")
        void userDeletionCascades() {
            repository.save(NotificationDeviceTestDataBuilder.aDevice().withOwner(USER_A).build());

            jdbcTemplate.update("DELETE FROM common.users WHERE id = ?", USER_A.uuid());

            assertThat(repository.findAll(NotificationDeviceFilter.all())).isEmpty();
        }
    }
}
