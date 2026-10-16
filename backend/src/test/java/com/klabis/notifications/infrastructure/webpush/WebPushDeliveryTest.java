package com.klabis.notifications.infrastructure.webpush;

import com.klabis.common.users.UserId;
import com.klabis.notifications.NotificationDeviceTestDataBuilder;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.application.NotificationDevicesPort;
import com.klabis.notifications.domain.NotificationDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("WebPushDelivery")
class WebPushDeliveryTest {

    private final UserId user = new UserId(UUID.randomUUID());
    private final PushMessage message = new PushMessage("t", "b", "/x");
    private final NotificationDevicesPort devicesPort = mock(NotificationDevicesPort.class);
    private final WebPushSender sender = mock(WebPushSender.class);
    private final NotificationDevice first = NotificationDeviceTestDataBuilder.aDevice().withOwner(user).build();
    private final NotificationDevice second = NotificationDeviceTestDataBuilder.aDevice().withOwner(user).build();
    private WebPushDelivery delivery;

    @BeforeEach
    void setUp() {
        when(devicesPort.listDevices(user)).thenReturn(List.of(first, second));
        when(sender.send(any(), any())).thenReturn(WebPushSender.Result.DELIVERED);
        delivery = new WebPushDelivery(devicesPort, sender);
    }

    @Test
    @DisplayName("sends to every device of the recipient")
    void sendsToAllDevices() {
        delivery.deliver(user, message);

        verify(sender).send(first, message);
        verify(sender).send(second, message);
        verify(devicesPort, never()).forgetEndpoint(any());
    }

    @Test
    @DisplayName("forgets the endpoint of a device the push service no longer knows")
    void forgetsGoneDevice() {
        when(sender.send(first, message)).thenReturn(WebPushSender.Result.GONE);

        delivery.deliver(user, message);

        verify(devicesPort).forgetEndpoint(first.getEndpoint());
        verify(devicesPort, never()).forgetEndpoint(second.getEndpoint());
    }

    @ParameterizedTest
    @EnumSource(value = WebPushSender.Result.class, names = "GONE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("keeps the device for any other outcome")
    void keepsDevice(WebPushSender.Result result) {
        when(sender.send(any(), any())).thenReturn(result);

        delivery.deliver(user, message);

        verify(devicesPort, never()).forgetEndpoint(any());
    }

    @Test
    @DisplayName("an unexpected failure does not propagate")
    void failuresAreContained() {
        doThrow(new IllegalStateException("boom")).when(sender).send(first, message);

        assertThatCode(() -> delivery.deliver(user, message)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a failing device lookup does not propagate")
    void lookupFailureIsContained() {
        when(devicesPort.listDevices(user)).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> delivery.deliver(user, message)).doesNotThrowAnyException();
    }
}
