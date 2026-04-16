package com.klabis.members.membersgroup.infrastructure.restapi;

import com.klabis.common.usergroup.Invitation;
import com.klabis.members.MemberId;
import com.klabis.members.membersgroup.application.PendingInvitationView;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
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

    static EntityModel<PendingInvitationResponse> build(MembersGroup group, Invitation invitation) {
        return buildInternal(group.getId(), group.getName(), invitation);
    }

    private static EntityModel<PendingInvitationResponse> buildInternal(MembersGroupId groupId, String groupName, Invitation invitation) {
        UUID groupUuid = groupId.uuid();
        UUID invitationUuid = invitation.getId().value();
        UUID invitedByUuid = MemberId.fromUserId(invitation.getInvitedBy()).uuid();
        UUID invitedMemberUuid = MemberId.fromUserId(invitation.getInvitedUser()).uuid();

        PendingInvitationResponse response = new PendingInvitationResponse(
                groupId, groupName, invitation.getId(), invitedByUuid);

        EntityModel<PendingInvitationResponse> model = EntityModel.of(response);
        model.add(Link.of("/api/members/" + invitedMemberUuid, "invitedMember"));
        klabisLinkTo(methodOn(MembersGroupController.class)
                .acceptInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("accept")
                        .andAffordances(klabisAfford(methodOn(MembersGroupController.class)
                                .acceptInvitation(groupUuid, invitationUuid, null)))));
        klabisLinkTo(methodOn(MembersGroupController.class)
                .rejectInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("reject")
                        .andAffordances(klabisAfford(methodOn(MembersGroupController.class)
                                .rejectInvitation(groupUuid, invitationUuid, null)))));
        return model;
    }
}
