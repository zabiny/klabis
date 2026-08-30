package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.groups.freegroup.domain.Invitation;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.infrastructure.restapi.GroupsApi;
import com.klabis.groups.infrastructure.restapi.PendingInvitationResponse;
import com.klabis.groups.infrastructure.restapi.PendingInvitationResponseBuilder;
import com.klabis.members.infrastructure.restapi.MembersApi;
import org.springframework.hateoas.EntityModel;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

// Only used for the excluded getGroup operation (FreeGroupController.buildPendingInvitationModel) —
// getPendingInvitations (PendingInvitationsController) builds its own EntityModel via
// PendingInvitationPostprocessor since it goes through HalResponseContext.setDomainList, not this
// hand-wrapping path.
class InvitationModelBuilder {

    private InvitationModelBuilder() {
    }

    static EntityModel<PendingInvitationResponse> build(FreeGroup group, Invitation invitation) {
        UUID groupUuid = group.getId().uuid();
        String groupName = group.getName();
        UUID invitationUuid = invitation.getId().value();
        UUID invitedByUuid = invitation.getInvitedBy().uuid();
        UUID invitedMemberUuid = invitation.getInvitedMember().uuid();

        PendingInvitationResponse response = PendingInvitationResponseBuilder.builder()
                .groupId(groupUuid)
                .groupName(groupName)
                .invitationId(invitationUuid)
                .invitedBy(invitedByUuid)
                .build();

        EntityModel<PendingInvitationResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(MembersApi.class).getMember(invitedMemberUuid, null))
                .map(link -> link.withRel("invitedMember"))
                .ifPresent(model::add);
        // TODO: "accept" and "reject" rels point to POST endpoints and should be pure affordances
        //   per backend-patterns skill. Kept as links because the frontend (GroupsPage.tsx) reads
        //   _links.accept / _links.reject directly to render action buttons.
        klabisLinkTo(methodOn(GroupsApi.class)
                .acceptInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("accept")
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                .acceptInvitation(groupUuid, invitationUuid, null)))));
        klabisLinkTo(methodOn(GroupsApi.class)
                .rejectInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("reject")
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                .rejectInvitation(groupUuid, invitationUuid, null)))));
        klabisLinkTo(methodOn(GroupsApi.class)
                .cancelInvitation(groupUuid, invitationUuid, null, null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                .cancelInvitation(groupUuid, invitationUuid, null, null)))));
        return model;
    }
}
