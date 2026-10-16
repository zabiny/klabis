package com.klabis.notifications.domain;

import com.klabis.common.users.UserId;
import org.jmolecules.ddd.annotation.ValueObject;

/**
 * All fields are optional — null means no restriction on that dimension.
 * Multiple non-null fields are combined with AND.
 */
@ValueObject
public record NotificationDeviceFilter(UserId owner, PushEndpoint endpoint) {

    public static NotificationDeviceFilter all() {
        return new NotificationDeviceFilter(null, null);
    }

    public NotificationDeviceFilter withOwner(UserId owner) {
        return new NotificationDeviceFilter(owner, this.endpoint);
    }

    public NotificationDeviceFilter withEndpoint(PushEndpoint endpoint) {
        return new NotificationDeviceFilter(this.owner, endpoint);
    }
}
