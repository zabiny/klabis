package com.klabis.notifications.application;

import com.klabis.common.users.UserId;
import com.klabis.notifications.domain.NotificationDevice;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface NotificationDevicesPort {

    List<NotificationDevice> listDevices(UserId owner);
}
