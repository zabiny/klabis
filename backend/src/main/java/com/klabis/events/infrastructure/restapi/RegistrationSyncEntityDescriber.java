package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.domain.RegistrationSyncId;
import com.klabis.members.MemberDto;
import com.klabis.members.Members;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.infrastructure.restapi.EntityDescription;
import com.klabis.sync.infrastructure.restapi.SyncEntityDescriber;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.Optional;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
class RegistrationSyncEntityDescriber implements SyncEntityDescriber {

    private final EventManagementPort eventManagement;
    private final Members members;

    RegistrationSyncEntityDescriber(EventManagementPort eventManagement, Members members) {
        this.eventManagement = eventManagement;
        this.members = members;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.EVENT_REGISTRATION;
    }

    @Override
    public Optional<EntityDescription> describe(String entityId) {
        RegistrationSyncId id = RegistrationSyncId.parse(entityId);
        Optional<MemberDto> member = members.findById(id.memberId());
        if (member.isEmpty()) {
            return Optional.empty();
        }
        try {
            var event = eventManagement.getEvent(id.eventId(), true);
            String label = member.get().firstName() + " " + member.get().lastName() + " – " + event.getName();
            return Optional.of(EntityDescription.of(label,
                    klabisLinkTo(methodOn(EventsApi.class).getEvent(id.eventId().value(), null))
                            .map(WebMvcLinkBuilder::withSelfRel)));
        } catch (EventNotFoundException e) {
            return Optional.empty();
        }
    }
}
