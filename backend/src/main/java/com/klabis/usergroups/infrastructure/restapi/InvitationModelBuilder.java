package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.usergroups.domain.Invitation;
import com.klabis.usergroups.domain.UserGroup;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

class InvitationModelBuilder {

    private InvitationModelBuilder() {
    }

    static EntityModel<PendingInvitationResponse> buildPendingInvitationModel(UserGroup group, Invitation invitation) {
        UUID groupUuid = group.getId().uuid();
        UUID invitationUuid = invitation.getId().value();
        UUID invitedByUuid = invitation.getInvitedBy().uuid();
        UUID invitedMemberUuid = invitation.getInvitedMember().uuid();

        PendingInvitationResponse response = new PendingInvitationResponse(
                groupUuid, group.getName(), invitationUuid, invitedByUuid);

        EntityModel<PendingInvitationResponse> model = EntityModel.of(response);
        model.add(Link.of("/api/members/" + invitedMemberUuid, "invitedMember"));
        klabisLinkTo(methodOn(InvitationController.class)
                .acceptInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("accept")
                        .andAffordances(klabisAfford(methodOn(InvitationController.class)
                                .acceptInvitation(groupUuid, invitationUuid, null)))));
        klabisLinkTo(methodOn(InvitationController.class)
                .rejectInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("reject")
                        .andAffordances(klabisAfford(methodOn(InvitationController.class)
                                .rejectInvitation(groupUuid, invitationUuid, null)))));
        return model;
    }
}
