package com.klabis.notifications.application;

import com.klabis.common.users.UserId;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.NotificationDeviceFilter;
import com.klabis.notifications.domain.NotificationDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class NotificationDevicesService implements NotificationDevicesPort {

    private final NotificationDeviceRepository deviceRepository;

    NotificationDevicesService(NotificationDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<NotificationDevice> listDevices(UserId owner) {
        return deviceRepository.findAll(NotificationDeviceFilter.all().withOwner(owner));
    }
}
