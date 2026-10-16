package com.klabis.notifications.application;

import com.klabis.common.users.UserId;
import com.klabis.notifications.NotificationDeviceTestDataBuilder;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.NotificationDeviceFilter;
import com.klabis.notifications.domain.NotificationDeviceRepository;
import com.klabis.notifications.domain.PushEndpoint;
import com.klabis.notifications.domain.PushKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationDevicesService.registerDevice")
class NotificationDevicesServiceTest {

    private static final UserId USER = new UserId(UUID.randomUUID());
    private static final UserId OTHER = new UserId(UUID.randomUUID());
    private static final String ENDPOINT = "https://push.example.org/send/abc";
    private static final String CHROME_ANDROID =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    @Mock
    private NotificationDeviceRepository repository;

    private NotificationDevicesService service() {
        return new NotificationDevicesService(repository);
    }

    private NotificationDevicesPort.RegisterDeviceCommand command(UserId owner, PushKeys keys) {
        return new NotificationDevicesPort.RegisterDeviceCommand(owner, new PushEndpoint(ENDPOINT), keys, CHROME_ANDROID);
    }

    @Test
    @DisplayName("creates a new device with description derived from the User-Agent")
    void createsNewDevice() {
        when(repository.findOne(NotificationDeviceFilter.all().withEndpoint(new PushEndpoint(ENDPOINT))))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationDevice result = service().registerDevice(command(USER, new PushKeys("k1", "a1")));

        assertThat(result.isOwnedBy(USER)).isTrue();
        assertThat(result.getDescription()).isEqualTo("Chrome · Android");
        assertThat(result.getKeys()).isEqualTo(new PushKeys("k1", "a1"));
        verify(repository).save(result);
    }

    @Test
    @DisplayName("updates keys when the same user registers the endpoint again")
    void updatesKeysForSameUser() {
        NotificationDevice existing = NotificationDeviceTestDataBuilder.aDevice()
                .withOwner(USER).withEndpoint(ENDPOINT).build();
        when(repository.findOne(NotificationDeviceFilter.all().withEndpoint(new PushEndpoint(ENDPOINT))))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationDevice result = service().registerDevice(command(USER, new PushKeys("k2", "a2")));

        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.isOwnedBy(USER)).isTrue();
        assertThat(result.getKeys()).isEqualTo(new PushKeys("k2", "a2"));
    }

    @Test
    @DisplayName("reassigns the device to the current user when another user owned the endpoint")
    void reassignsToCurrentUser() {
        NotificationDevice existing = NotificationDeviceTestDataBuilder.aDevice()
                .withOwner(OTHER).withEndpoint(ENDPOINT).build();
        when(repository.findOne(NotificationDeviceFilter.all().withEndpoint(new PushEndpoint(ENDPOINT))))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationDevice result = service().registerDevice(command(USER, new PushKeys("k2", "a2")));

        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.isOwnedBy(USER)).isTrue();
        assertThat(result.getKeys()).isEqualTo(new PushKeys("k2", "a2"));
    }

    @Test
    @DisplayName("removeDevice deletes a device resolved through its owner")
    void removesOwnedDevice() {
        NotificationDevice device = NotificationDeviceTestDataBuilder.aDevice().withOwner(USER).build();
        when(repository.findOne(NotificationDeviceFilter.all().withOwner(USER).withId(device.getId())))
                .thenReturn(Optional.of(device));

        service().removeDevice(USER, device.getId());

        verify(repository).delete(device);
    }

    @Test
    @DisplayName("removeDevice treats an unknown or foreign device as not found")
    void removeUnknownDevice() {
        var id = com.klabis.notifications.domain.NotificationDeviceId.generate();
        when(repository.findOne(NotificationDeviceFilter.all().withOwner(OTHER).withId(id))).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().removeDevice(OTHER, id))
                .isInstanceOf(NotificationDeviceNotFoundException.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).delete(any());
    }
}
