package com.klabis.notifications.domain;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;

@SecondaryPort
public interface NotificationDeviceRepository {

    NotificationDevice save(NotificationDevice device);

    List<NotificationDevice> findAll(NotificationDeviceFilter filter);

    Optional<NotificationDevice> findOne(NotificationDeviceFilter filter);
}
