package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.usergroup.Invitation;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.application.PendingInvitationView;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.members.MemberId;
import com.klabis.groups.freegroup.infrastructure.restapi.FreeGroupController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

class InvitationModelBuilder {

    private InvitationModelBuilder() {
    }

    static EntityModel<PendingInvitationResponse> buildFromView(PendingInvitationView view) {
        return buildInternal(view.groupId(), view.groupName(), view.invitation());
    }

    static EntityModel<PendingInvitationResponse> build(FreeGroup group, Invitation invitation) {
        return buildInternal(group.getId(), group.getName(), invitation);
    }

    private static EntityModel<PendingInvitationResponse> buildInternal(FreeGroupId groupId, String groupName, Invitation invitation) {
        UUID groupUuid = groupId.uuid();
        UUID invitationUuid = invitation.getId().value();
        UUID invitedByUuid = MemberId.fromUserId(invitation.getInvitedBy()).uuid();
        UUID invitedMemberUuid = MemberId.fromUserId(invitation.getInvitedUser()).uuid();

        PendingInvitationResponse response = new PendingInvitationResponse(
                groupId, groupName, invitation.getId(), invitedByUuid);

        EntityModel<PendingInvitationResponse> model = EntityModel.of(response);
        model.add(Link.of("/api/members/" + invitedMemberUuid, "invitedMember"));
        klabisLinkTo(methodOn(FreeGroupController.class)
                .acceptInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("accept")
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class)
                                .acceptInvitation(groupUuid, invitationUuid, null)))));
        klabisLinkTo(methodOn(FreeGroupController.class)
                .rejectInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("reject")
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class)
                                .rejectInvitation(groupUuid, invitationUuid, null)))));
        return model;
    }
}
