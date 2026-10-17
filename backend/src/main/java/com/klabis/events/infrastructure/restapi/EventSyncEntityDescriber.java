package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.infrastructure.restapi.EntityDescription;
import com.klabis.sync.infrastructure.restapi.SyncEntityDescriber;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
class EventSyncEntityDescriber implements SyncEntityDescriber {

    private final EventManagementPort eventManagement;

    EventSyncEntityDescriber(EventManagementPort eventManagement) {
        this.eventManagement = eventManagement;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.EVENT;
    }

    @Override
    public Optional<EntityDescription> describe(String entityId) {
        UUID eventUuid = UUID.fromString(entityId);
        try {
            var event = eventManagement.getEvent(new EventId(eventUuid), true);
            return Optional.of(EntityDescription.of(
                    SyncEntityLabels.event(event),
                    klabisLinkTo(methodOn(EventsApi.class).getEvent(eventUuid, null))
                            .map(WebMvcLinkBuilder::withSelfRel)));
        } catch (EventNotFoundException e) {
            return Optional.empty();
        }
    }
}
