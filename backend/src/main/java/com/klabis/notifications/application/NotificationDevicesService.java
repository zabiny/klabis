package com.klabis.notifications.application;

import com.klabis.common.users.UserId;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.NotificationDeviceFilter;
import com.klabis.notifications.domain.NotificationDeviceId;
import com.klabis.notifications.domain.NotificationDeviceRepository;
import com.klabis.notifications.domain.PushEndpoint;
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

    @Transactional
    @Override
    public NotificationDevice registerDevice(RegisterDeviceCommand command) {
        NotificationDevice device = deviceRepository
                .findOne(NotificationDeviceFilter.all().withEndpoint(command.endpoint()))
                .orElseGet(() -> NotificationDevice.registerFor(command.owner(), command.endpoint(), command.keys(),
                        UserAgentDescriber.describe(command.userAgent())));
        device.reassignTo(command.owner());
        device.updateKeys(command.keys());
        return deviceRepository.save(device);
    }

    @Transactional
    @Override
    public void removeDevice(UserId owner, NotificationDeviceId deviceId) {
        NotificationDevice device = deviceRepository
                .findOne(NotificationDeviceFilter.all().withOwner(owner).withId(deviceId))
                .orElseThrow(() -> new NotificationDeviceNotFoundException(deviceId));
        deviceRepository.delete(device);
    }

    @Transactional
    @Override
    public void forgetEndpoint(PushEndpoint endpoint) {
        deviceRepository.findOne(NotificationDeviceFilter.all().withEndpoint(endpoint))
                .ifPresent(deviceRepository::delete);
    }
}
