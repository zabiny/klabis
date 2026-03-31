package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.exceptions.MemberProfileRequiredException;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.InvitationPort;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.InvitationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@Tag(name = "Invitations", description = "Free group invitation management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class InvitationController {

    private final InvitationPort invitationService;

    InvitationController(InvitationPort invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping(value = "/api/groups/{groupId}/invitations", consumes = "application/json",
            produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "Invite a member to a free group (owner only)")
    ResponseEntity<Void> inviteMember(
            @Parameter(description = "Group UUID") @PathVariable UUID groupId,
            @Valid @RequestBody InviteMemberRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        UserGroupId userGroupId = new UserGroupId(groupId);
        invitationService.inviteMember(userGroupId, currentUser.memberId(), request.memberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/api/groups/{groupId}/invitations/{invitationId}/accept",
            produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "Accept a pending invitation")
    ResponseEntity<Void> acceptInvitation(
            @Parameter(description = "Group UUID") @PathVariable UUID groupId,
            @Parameter(description = "Invitation UUID") @PathVariable UUID invitationId,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        UserGroupId userGroupId = new UserGroupId(groupId);
        InvitationId id = new InvitationId(invitationId);
        invitationService.acceptInvitation(userGroupId, id, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/api/groups/{groupId}/invitations/{invitationId}/reject",
            produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "Reject a pending invitation")
    ResponseEntity<Void> rejectInvitation(
            @Parameter(description = "Group UUID") @PathVariable UUID groupId,
            @Parameter(description = "Invitation UUID") @PathVariable UUID invitationId,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        UserGroupId userGroupId = new UserGroupId(groupId);
        InvitationId id = new InvitationId(invitationId);
        invitationService.rejectInvitation(userGroupId, id, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/api/invitations/pending", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "List current user's pending invitations across all groups")
    ResponseEntity<CollectionModel<EntityModel<PendingInvitationResponse>>> getPendingInvitations(
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        List<FreeGroup> groups = invitationService.getGroupsWithPendingInvitations(currentUser.memberId());
        List<EntityModel<PendingInvitationResponse>> items = groups.stream()
                .flatMap(group -> group.getPendingInvitations().stream()
                        .filter(inv -> inv.getInvitedMember().equals(currentUser.memberId()))
                        .map(inv -> InvitationModelBuilder.buildPendingInvitationModel(group, inv)))
                .toList();

        CollectionModel<EntityModel<PendingInvitationResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(InvitationController.class).getPendingInvitations(null))
                .ifPresent(link -> model.add(link.withSelfRel()));

        return ResponseEntity.ok(model);
    }

    private void requireMemberProfile(CurrentUserData currentUser) {
        if (!currentUser.isMember()) {
            throw new MemberProfileRequiredException();
        }
    }
}
