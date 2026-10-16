package com.klabis.notifications.infrastructure.webpush;

import com.klabis.common.users.UserId;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.application.NotificationDevicesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

class WebPushDelivery {

    private static final Logger log = LoggerFactory.getLogger(WebPushDelivery.class);

    private final NotificationDevicesPort devicesPort;
    private final WebPushSender sender;

    WebPushDelivery(NotificationDevicesPort devicesPort, WebPushSender sender) {
        this.devicesPort = devicesPort;
        this.sender = sender;
    }

    @Async
    public void deliver(UserId recipient, PushMessage message) {
        try {
            devicesPort.listDevices(recipient).forEach(device -> {
                if (sender.send(device, message) == WebPushSender.Result.GONE) {
                    devicesPort.forgetEndpoint(device.getEndpoint());
                }
            });
        } catch (RuntimeException e) {
            log.warn("Push notification for user {} failed: {}", recipient, e.toString());
        }
    }
}
