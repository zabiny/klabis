package com.klabis.notifications.application;

import com.klabis.common.users.UserId;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.NotificationDeviceId;
import com.klabis.notifications.domain.PushEndpoint;
import com.klabis.notifications.domain.PushKeys;
import org.jspecify.annotations.Nullable;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface NotificationDevicesPort {

    List<NotificationDevice> listDevices(UserId owner);

    NotificationDevice registerDevice(RegisterDeviceCommand command);

    void removeDevice(UserId owner, NotificationDeviceId deviceId);

    void forgetEndpoint(PushEndpoint endpoint);

    record RegisterDeviceCommand(UserId owner, PushEndpoint endpoint, PushKeys keys, @Nullable String userAgent) {
    }
}
