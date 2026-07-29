package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.users.Authority;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.application.PendingInvitationView;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

// PendingInvitationResponse items here never carry the owner-only "self"+cancelInvitation
// affordance that the same DTO carries when embedded inside GroupResponse — see
// EntityModelPendingInvitationResponseForInvitationsList in groups.yaml. Links are added by
// PendingInvitationPostprocessor below, keyed off the domain PendingInvitationView.

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Invitations", description = "Invitation management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.GROUPS_SCOPE})
class PendingInvitationsController implements InvitationsApi {

    private final FreeGroupManagementPort membersGroupManagementService;

    PendingInvitationsController(FreeGroupManagementPort membersGroupManagementService) {
        this.membersGroupManagementService = membersGroupManagementService;
    }

    @Override
    public ResponseEntity<Collection<PendingInvitationResponse>> getPendingInvitations(
            @ActingMember MemberId actingMember) {

        List<PendingInvitationView> views = membersGroupManagementService.getPendingInvitationsForMember(actingMember);

        HalResponseContext.setDomainList(views);
        return ResponseEntity.ok(views.stream()
                .map(view -> new PendingInvitationResponse(
                        view.groupId(), view.groupName(), view.invitation().getId(),
                        view.invitation().getInvitedBy().uuid()))
                .toList());
    }
}

@MvcComponent
class PendingInvitationPostprocessor
        extends ModelWithDomainPostprocessor<PendingInvitationResponse, PendingInvitationView> {

    @Override
    public void process(EntityModel<PendingInvitationResponse> dtoModel, PendingInvitationView view) {
        var invitation = view.invitation();
        java.util.UUID groupUuid = view.groupId().uuid();
        java.util.UUID invitationUuid = invitation.getId().value();
        java.util.UUID invitedMemberUuid = invitation.getInvitedMember().uuid();

        klabisLinkTo(methodOn(MemberController.class).getMember(invitedMemberUuid, null))
                .map(link -> link.withRel("invitedMember"))
                .ifPresent(dtoModel::add);
        // TODO: "accept" and "reject" rels point to POST endpoints and should be pure affordances
        //   per backend-patterns skill. Kept as links because the frontend (GroupsPage.tsx) reads
        //   _links.accept / _links.reject directly to render action buttons — same as
        //   InvitationModelBuilder.build, which this postprocessor mirrors for this collection.
        klabisLinkTo(methodOn(FreeGroupController.class).acceptInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> dtoModel.add(link.withRel("accept")
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class)
                                .acceptInvitation(groupUuid, invitationUuid, null)))));
        klabisLinkTo(methodOn(FreeGroupController.class).rejectInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> dtoModel.add(link.withRel("reject")
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class)
                                .rejectInvitation(groupUuid, invitationUuid, null)))));
    }
}
