package com.klabis.notifications.infrastructure.webpush;

import com.klabis.common.users.UserId;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.PushNotifications;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class WebPushNotifications implements PushNotifications {

    private final WebPushDelivery delivery;

    WebPushNotifications(WebPushDelivery delivery) {
        this.delivery = delivery;
    }

    @Override
    public void send(UserId recipient, PushMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    delivery.deliver(recipient, message);
                }
            });
        } else {
            delivery.deliver(recipient, message);
        }
    }
}
