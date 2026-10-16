package com.klabis.notifications.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.ActingUser;
import com.klabis.common.users.UserId;
import com.klabis.notifications.application.NotificationDevicesPort;
import com.klabis.notifications.application.VapidProperties;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class NotificationController implements NotificationsApi {

    private final NotificationDevicesPort devicesPort;
    private final VapidProperties vapidProperties;

    NotificationController(NotificationDevicesPort devicesPort, VapidProperties vapidProperties) {
        this.devicesPort = devicesPort;
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
}

@MvcComponent
class NotificationSettingsPostprocessor implements RepresentationModelProcessor<EntityModel<NotificationSettings>> {

    @Override
    public EntityModel<NotificationSettings> process(EntityModel<NotificationSettings> model) {
        klabisLinkTo(methodOn(NotificationsApi.class).getNotificationSettings(null))
                .ifPresent(link -> model.add(link.withSelfRel()));
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
