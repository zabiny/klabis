package com.klabis.notifications.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.ActingUser;
import com.klabis.common.users.UserId;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.PushNotifications;
import com.klabis.notifications.application.NotificationDevicesPort;
import com.klabis.notifications.application.NotificationDevicesPort.RegisterDeviceCommand;
import com.klabis.notifications.application.VapidProperties;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.NotificationDeviceId;
import com.klabis.notifications.domain.PushEndpoint;
import com.klabis.notifications.domain.PushKeys;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class NotificationController implements NotificationsApi {

    private final NotificationDevicesPort devicesPort;
    private final VapidProperties vapidProperties;
    private final PushNotifications pushNotifications;

    NotificationController(NotificationDevicesPort devicesPort, VapidProperties vapidProperties,
                           PushNotifications pushNotifications) {
        this.devicesPort = devicesPort;
        this.pushNotifications = pushNotifications;
        this.vapidProperties = vapidProperties;
    }

    @Override
    public ResponseEntity<NotificationSettings> getNotificationSettings(@ActingUser UserId currentUserId) {
        var devices = devicesPort.listDevices(currentUserId).stream()
                .map(device -> EntityModel.of(NotificationDeviceBuilder.builder()
                        .id(device.getId().value())
                        .description(device.getDescription())
                        .enabledAt(device.getEnabledAt())
                        .endpoint(device.getEndpoint().url())
                        .build()))
                .toList();

        NotificationSettings settings = NotificationSettingsBuilder.builder()
                .vapidPublicKey(vapidProperties.publicKey())
                .devices(devices)
                .build();

        HalResponseContext.setDomain(settings);
        return ResponseEntity.ok(settings);
    }

    @Override
    public ResponseEntity<Void> registerNotificationDevice(RegisterNotificationDeviceRequest request,
                                                           @ActingUser UserId currentUserId, String userAgent) {
        NotificationDevice device = devicesPort.registerDevice(new RegisterDeviceCommand(
                currentUserId,
                new PushEndpoint(request.endpoint()),
                new PushKeys(request.p256dh(), request.auth()),
                userAgent));

        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{deviceId}")
                .buildAndExpand(device.getId().value())
                .toUri()).build();
    }

    @Override
    public ResponseEntity<Void> removeNotificationDevice(UUID deviceId, @ActingUser UserId currentUserId) {
        devicesPort.removeDevice(currentUserId, new NotificationDeviceId(deviceId));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> sendTestNotification(@ActingUser UserId currentUserId) {
        pushNotifications.send(currentUserId, new PushMessage("Notifikace fungují",
                "Na tomto zařízení budete dostávat oznámení z Klabisu.", "/notifications"));
        return ResponseEntity.noContent().build();
    }
}

@MvcComponent
class NotificationSettingsPostprocessor implements RepresentationModelProcessor<EntityModel<NotificationSettings>> {

    @Override
    public EntityModel<NotificationSettings> process(EntityModel<NotificationSettings> model) {
        boolean hasDevice = model.getContent() != null && !model.getContent().devices().isEmpty();
        klabisLinkTo(methodOn(NotificationsApi.class).getNotificationSettings(null))
                .ifPresent(link -> {
                    var affordances = new ArrayList<>(klabisAfford(
                            methodOn(NotificationsApi.class).registerNotificationDevice(null, null, null)));
                    if (hasDevice) {
                        affordances.addAll(klabisAfford(methodOn(NotificationsApi.class).sendTestNotification(null)));
                    }
                    model.add(link.withSelfRel().andAffordances(affordances));
                });
        return model;
    }
}

@MvcComponent
class NotificationsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    // MvcComponents are scanned in every web slice, including those that do not bind properties.
    private final ObjectProvider<VapidProperties> vapidProperties;

    NotificationsRootPostprocessor(ObjectProvider<VapidProperties> vapidProperties) {
        this.vapidProperties = vapidProperties;
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        VapidProperties properties = vapidProperties.getIfAvailable();
        if (properties == null || !properties.isConfigured()) {
            return model;
        }
        klabisLinkTo(methodOn(NotificationsApi.class).getNotificationSettings(null))
                .ifPresent(link -> model.add(link.withRel("notifications")));
        return model;
    }
}
