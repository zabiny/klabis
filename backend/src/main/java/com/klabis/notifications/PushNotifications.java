package com.klabis.notifications;

import com.klabis.common.users.UserId;

public interface PushNotifications {

    void send(UserId recipient, PushMessage message);
}
