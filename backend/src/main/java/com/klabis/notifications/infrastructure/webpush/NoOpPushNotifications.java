package com.klabis.notifications.infrastructure.webpush;

import com.klabis.common.users.UserId;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.PushNotifications;

class NoOpPushNotifications implements PushNotifications {

    @Override
    public void send(UserId recipient, PushMessage message) {
    }
}
