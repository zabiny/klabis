package com.klabis.notifications.application;

import com.klabis.common.users.UserId;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.PushEndpoint;
import com.klabis.notifications.domain.PushKeys;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface NotificationDevicesPort {

    List<NotificationDevice> listDevices(UserId owner);

    NotificationDevice registerDevice(RegisterDeviceCommand command);

    record RegisterDeviceCommand(UserId owner, PushEndpoint endpoint, PushKeys keys, @Nullable String userAgent) {

        public RegisterDeviceCommand {
            Assert.notNull(owner, "Owner is required");
            Assert.notNull(endpoint, "Endpoint is required");
            Assert.notNull(keys, "Keys are required");
        }
    }
}
