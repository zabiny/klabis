package com.klabis.notifications.infrastructure.webpush;

import com.klabis.common.users.UserId;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.PushNotifications;
import com.klabis.notifications.application.NotificationDevicesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

class WebPushNotifications implements PushNotifications {

    private static final Logger log = LoggerFactory.getLogger(WebPushNotifications.class);

    private final Executor executor;
    private final NotificationDevicesPort devicesPort;
    private final WebPushSender sender;

    WebPushNotifications(Executor executor, NotificationDevicesPort devicesPort, WebPushSender sender) {
        this.executor = executor;
        this.devicesPort = devicesPort;
        this.sender = sender;
    }

    @Override
    public void send(UserId recipient, PushMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(recipient, message);
                }
            });
        } else {
            dispatch(recipient, message);
        }
    }

    private void dispatch(UserId recipient, PushMessage message) {
        try {
            executor.execute(() -> deliver(recipient, message));
        } catch (RuntimeException e) {
            log.warn("Push notification for user {} could not be scheduled: {}", recipient, e.toString());
        }
    }

    private void deliver(UserId recipient, PushMessage message) {
        try {
            devicesPort.listDevices(recipient).forEach(device -> {
                try {
                    int status = sender.send(device, message);
                    if (status == 404 || status == 410) {
                        devicesPort.removeDevice(device.getOwner(), device.getId());
                    }
                } catch (RuntimeException e) {
                    log.warn("Push to device {} failed: {}", device.getId(), e.toString());
                }
            });
        } catch (RuntimeException e) {
            log.warn("Push notification for user {} failed: {}", recipient, e.toString());
        }
    }
}
