package com.klabis.notifications.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.notifications.domain.NotificationDeviceId;

public class NotificationDeviceNotFoundException extends ResourceNotFoundException {

    public NotificationDeviceNotFoundException(NotificationDeviceId id) {
        super("Notification device not found with ID: " + id);
    }
}
