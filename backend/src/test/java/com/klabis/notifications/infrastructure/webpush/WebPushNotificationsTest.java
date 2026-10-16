package com.klabis.notifications.infrastructure.webpush;

import com.klabis.common.users.UserId;
import com.klabis.notifications.PushMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("WebPushNotifications")
class WebPushNotificationsTest {

    private final UserId user = new UserId(UUID.randomUUID());
    private final PushMessage message = new PushMessage("t", "b", "/x");
    private final WebPushDelivery delivery = mock(WebPushDelivery.class);
    private final WebPushNotifications notifications = new WebPushNotifications(delivery);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("delivers immediately when no transaction is active")
    void deliversImmediatelyWithoutTransaction() {
        notifications.send(user, message);

        verify(delivery).deliver(user, message);
    }

    @Test
    @DisplayName("waits for the surrounding transaction to commit")
    void deliversAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        notifications.send(user, message);

        verify(delivery, never()).deliver(any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(delivery).deliver(user, message);
    }
}
