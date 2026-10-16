package com.klabis.notifications.domain;

import com.klabis.common.users.UserId;
import com.klabis.notifications.NotificationDeviceTestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificationDevice")
class NotificationDeviceTest {

    private static final UserId OWNER = new UserId(UUID.randomUUID());
    private static final UserId OTHER = new UserId(UUID.randomUUID());

    @Nested
    @DisplayName("registerFor()")
    class RegisterFor {

        @Test
        @DisplayName("creates a device owned by the user with generated id and enabledAt")
        void createsDevice() {
            Instant before = Instant.now();

            NotificationDevice device = NotificationDevice.registerFor(
                    OWNER,
                    new PushEndpoint("https://push.example.org/1"),
                    new PushKeys("p256dh", "auth"),
                    "Firefox · Linux");

            assertThat(device.getId()).isNotNull();
            assertThat(device.isOwnedBy(OWNER)).isTrue();
            assertThat(device.getEndpoint().url()).isEqualTo("https://push.example.org/1");
            assertThat(device.getKeys()).isEqualTo(new PushKeys("p256dh", "auth"));
            assertThat(device.getDescription()).isEqualTo("Firefox · Linux");
            assertThat(device.getEnabledAt()).isBetween(before, Instant.now());
        }

        @Test
        @DisplayName("requires owner, endpoint, keys and description")
        void requiresAll() {
            var endpoint = new PushEndpoint("https://push.example.org/1");
            var keys = new PushKeys("p256dh", "auth");

            assertThatThrownBy(() -> NotificationDevice.registerFor(null, endpoint, keys, "d"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> NotificationDevice.registerFor(OWNER, null, keys, "d"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> NotificationDevice.registerFor(OWNER, endpoint, null, "d"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> NotificationDevice.registerFor(OWNER, endpoint, keys, " "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reassignTo()")
    class ReassignTo {

        @Test
        @DisplayName("moves the device to another user and restarts enabledAt")
        void movesDevice() throws Exception {
            NotificationDevice device = NotificationDeviceTestDataBuilder.aDevice().withOwner(OWNER).build();
            Instant originalEnabledAt = device.getEnabledAt();
            Thread.sleep(2);

            device.reassignTo(OTHER);

            assertThat(device.isOwnedBy(OTHER)).isTrue();
            assertThat(device.isOwnedBy(OWNER)).isFalse();
            assertThat(device.getEnabledAt()).isAfter(originalEnabledAt);
        }

        @Test
        @DisplayName("keeps the device untouched when the owner does not change")
        void sameOwnerIsNoOp() {
            NotificationDevice device = NotificationDeviceTestDataBuilder.aDevice().withOwner(OWNER).build();
            Instant originalEnabledAt = device.getEnabledAt();

            device.reassignTo(OWNER);

            assertThat(device.isOwnedBy(OWNER)).isTrue();
            assertThat(device.getEnabledAt()).isEqualTo(originalEnabledAt);
        }
    }

    @Nested
    @DisplayName("updateKeys()")
    class UpdateKeys {

        @Test
        @DisplayName("replaces the push keys")
        void replacesKeys() {
            NotificationDevice device = NotificationDeviceTestDataBuilder.aDevice().build();

            device.updateKeys(new PushKeys("newP256dh", "newAuth"));

            assertThat(device.getKeys()).isEqualTo(new PushKeys("newP256dh", "newAuth"));
        }

        @Test
        @DisplayName("requires keys")
        void requiresKeys() {
            NotificationDevice device = NotificationDeviceTestDataBuilder.aDevice().build();

            assertThatThrownBy(() -> device.updateKeys(null)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
