package com.klabis.notifications;

import com.klabis.common.users.UserId;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.PushEndpoint;
import com.klabis.notifications.domain.PushKeys;

import java.util.UUID;

public class NotificationDeviceTestDataBuilder {

    private UserId owner = new UserId(UUID.randomUUID());
    private PushEndpoint endpoint = new PushEndpoint("https://push.example.org/send/" + UUID.randomUUID());
    private PushKeys keys = new PushKeys("BPublicKeyValue", "authSecret");
    private String description = "Chrome · Android";

    public static NotificationDeviceTestDataBuilder aDevice() {
        return new NotificationDeviceTestDataBuilder();
    }

    public NotificationDeviceTestDataBuilder withOwner(UserId owner) {
        this.owner = owner;
        return this;
    }

    public NotificationDeviceTestDataBuilder withEndpoint(String url) {
        this.endpoint = new PushEndpoint(url);
        return this;
    }

    public NotificationDeviceTestDataBuilder withKeys(PushKeys keys) {
        this.keys = keys;
        return this;
    }

    public NotificationDeviceTestDataBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public NotificationDevice build() {
        return NotificationDevice.registerFor(owner, endpoint, keys, description);
    }
}
