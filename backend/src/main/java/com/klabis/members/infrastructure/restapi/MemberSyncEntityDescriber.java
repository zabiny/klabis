package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.infrastructure.restapi.EntityDescription;
import com.klabis.sync.infrastructure.restapi.SyncEntityDescriber;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
class MemberSyncEntityDescriber implements SyncEntityDescriber {

    private final Members members;

    MemberSyncEntityDescriber(Members members) {
        this.members = members;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.MEMBER;
    }

    @Override
    public Optional<EntityDescription> describe(String entityId) {
        UUID memberUuid = UUID.fromString(entityId);
        return members.findById(new MemberId(memberUuid)).map(member -> {
            String label = member.firstName() + " " + member.lastName() + " (" + member.registrationNumber() + ")";
            return EntityDescription.of(label,
                    klabisLinkTo(methodOn(MembersApi.class).getMember(memberUuid, null))
                            .map(WebMvcLinkBuilder::withSelfRel));
        });
    }
}
