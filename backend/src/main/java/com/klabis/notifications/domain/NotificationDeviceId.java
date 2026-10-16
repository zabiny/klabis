package com.klabis.notifications.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record NotificationDeviceId(UUID value) implements Identifier {

    public NotificationDeviceId {
        if (value == null) {
            throw new IllegalArgumentException("Notification device ID is required");
        }
    }

    public static NotificationDeviceId generate() {
        return new NotificationDeviceId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
