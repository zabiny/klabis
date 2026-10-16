package com.klabis.notifications.infrastructure.webpush;

import com.klabis.common.users.UserId;
import com.klabis.notifications.NotificationDeviceTestDataBuilder;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.application.NotificationDevicesPort;
import com.klabis.notifications.domain.NotificationDevice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@DisplayName("WebPushNotifications")
class WebPushNotificationsTest {

    private final UserId user = new UserId(UUID.randomUUID());
    private final PushMessage message = new PushMessage("t", "b", "/x");
    private final NotificationDevicesPort devicesPort = mock(NotificationDevicesPort.class);
    private final WebPushSender sender = mock(WebPushSender.class);
    private final NotificationDevice first = NotificationDeviceTestDataBuilder.aDevice().withOwner(user).build();
    private final NotificationDevice second = NotificationDeviceTestDataBuilder.aDevice().withOwner(user).build();
    private final Executor sameThread = Runnable::run;
    private WebPushNotifications notifications;

    @BeforeEach
    void setUp() {
        when(devicesPort.listDevices(user)).thenReturn(List.of(first, second));
        notifications = new WebPushNotifications(sameThread, devicesPort, sender);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("sends to every device of the recipient immediately when no transaction is active")
    void sendsImmediatelyWithoutTransaction() {
        notifications.send(user, message);

        verify(sender).send(first, message);
        verify(sender).send(second, message);
    }

    @Test
    @DisplayName("waits for the surrounding transaction to commit")
    void sendsAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        notifications.send(user, message);

        verify(sender, never()).send(any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(sender).send(first, message);
        verify(sender).send(second, message);
    }

    @Test
    @DisplayName("a failing device does not stop delivery to the others nor propagate")
    void failuresAreContained() {
        doThrow(new IllegalStateException("boom")).when(sender).send(first, message);

        assertThatCode(() -> notifications.send(user, message)).doesNotThrowAnyException();

        verify(sender).send(second, message);
    }

    @Test
    @DisplayName("a failing device lookup does not propagate")
    void lookupFailureIsContained() {
        when(devicesPort.listDevices(user)).thenThrow(new IllegalStateException("db down"));

        assertThatCode(() -> notifications.send(user, message)).doesNotThrowAnyException();
    }
}
