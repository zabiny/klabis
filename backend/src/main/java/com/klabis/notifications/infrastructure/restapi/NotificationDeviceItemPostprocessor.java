package com.klabis.notifications.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
class NotificationDeviceItemPostprocessor implements RepresentationModelProcessor<EntityModel<NotificationDevice>> {

    @Override
    public EntityModel<NotificationDevice> process(EntityModel<NotificationDevice> model) {
        UUID deviceId = model.getContent().id();
        klabisLinkTo(methodOn(NotificationsApi.class).removeNotificationDevice(deviceId, null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(NotificationsApi.class)
                                .removeNotificationDevice(deviceId, null)))));
        return model;
    }
}
