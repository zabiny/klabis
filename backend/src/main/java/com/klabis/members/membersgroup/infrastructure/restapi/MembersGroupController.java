package com.klabis.members.membersgroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.Invitation;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.members.ActingMember;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.membersgroup.application.MembersGroupManagementPort;
import com.klabis.members.membersgroup.domain.GroupOwnershipRequiredException;
import com.klabis.members.membersgroup.domain.MembersGroup;
import com.klabis.members.membersgroup.domain.MembersGroupId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Groups", description = "Members group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class MembersGroupController {

    private final MembersGroupManagementPort membersGroupManagementService;

    MembersGroupController(MembersGroupManagementPort membersGroupManagementService) {
        this.membersGroupManagementService = membersGroupManagementService;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create a members group")
    ResponseEntity<Void> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @ActingMember MemberId actingMember) {

        MembersGroup group = membersGroupManagementService.createGroup(request.name(), actingMember);

        return ResponseEntity.created(
                linkTo(methodOn(MembersGroupController.class).getGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List groups where authenticated member is a member")
    ResponseEntity<CollectionModel<EntityModel<GroupSummaryResponse>>> listGroups(
            @ActingMember MemberId actingMember) {

        List<MembersGroup> groups = membersGroupManagementService.listGroupsForMember(actingMember);
        List<EntityModel<GroupSummaryResponse>> items = groups.stream()
                .map(this::buildGroupSummaryModel)
                .toList();

        CollectionModel<EntityModel<GroupSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(MembersGroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(MembersGroupController.class).createGroup(null, null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group details")
    ResponseEntity<EntityModel<GroupResponse>> getGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @ActingUser CurrentUserData currentUser) {

        MembersGroupId groupId = new MembersGroupId(id);
        MembersGroup group = membersGroupManagementService.getGroup(groupId);

        MemberId requestingMember = currentUser != null ? currentUser.memberId() : null;
        boolean isOwner = requestingMember != null && group.isOwner(requestingMember);

        GroupResponse response = toGroupResponse(group, id, isOwner);
        EntityModel<GroupResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(MembersGroupController.class).getGroup(id, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel();
            if (isOwner) {
                selfLink = selfLink
                        .andAffordances(klabisAfford(methodOn(MembersGroupController.class).updateGroup(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(MembersGroupController.class).deleteGroup(id, null)))
                        .andAffordances(klabisAfford(methodOn(MembersGroupController.class).addGroupOwner(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(MembersGroupController.class).inviteMember(id, null, null)));
            }
            model.add(selfLink);
        });

        klabisLinkTo(methodOn(MembersGroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withRel("collection")));

        return ResponseEntity.ok(model);
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Rename a group (owner only)")
    ResponseEntity<Void> updateGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody RenameGroupRequest request,
            @ActingMember MemberId actingMember) {

        MembersGroupId groupId = new MembersGroupId(id);
        requireOwnership(groupId, actingMember);
        membersGroupManagementService.renameGroup(groupId, request.name());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a group (owner only)")
    ResponseEntity<Void> deleteGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @ActingMember MemberId actingMember) {

        MembersGroupId groupId = new MembersGroupId(id);
        requireOwnership(groupId, actingMember);
        membersGroupManagementService.deleteGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from group (owner only)")
    ResponseEntity<Void> removeGroupMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @ActingMember MemberId actingMember) {

        MembersGroupId groupId = new MembersGroupId(id);
        requireOwnership(groupId, actingMember);
        membersGroupManagementService.removeMember(groupId, new MemberId(memberId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/owners", consumes = "application/json")
    @Operation(summary = "Add an owner to group (owner only)")
    ResponseEntity<Void> addGroupOwner(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddOwnerRequest request,
            @ActingMember MemberId actingMember) {

        MembersGroupId groupId = new MembersGroupId(id);
        requireOwnership(groupId, actingMember);
        membersGroupManagementService.addOwner(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/owners/{memberId}")
    @Operation(summary = "Remove an owner from group (owner only)")
    ResponseEntity<Void> removeGroupOwner(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Owner member UUID") @PathVariable UUID memberId,
            @ActingMember MemberId actingMember) {

        MembersGroupId groupId = new MembersGroupId(id);
        requireOwnership(groupId, actingMember);
        membersGroupManagementService.removeOwner(groupId, new MemberId(memberId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/invitations", consumes = "application/json",
            produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "Invite a member to a members group (owner only)")
    ResponseEntity<Void> inviteMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest request,
            @ActingMember MemberId actingMember) {

        MembersGroupId groupId = new MembersGroupId(id);
        requireOwnership(groupId, actingMember);
        membersGroupManagementService.inviteMember(groupId, actingMember, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/invitations/{invitationId}/accept",
            produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "Accept a pending invitation")
    ResponseEntity<Void> acceptInvitation(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Invitation UUID") @PathVariable UUID invitationId,
            @ActingMember MemberId actingMember) {

        MembersGroupId groupId = new MembersGroupId(id);
        InvitationId invId = new InvitationId(invitationId);
        membersGroupManagementService.acceptInvitation(groupId, invId, actingMember);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/invitations/{invitationId}/reject",
            produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "Reject a pending invitation")
    ResponseEntity<Void> rejectInvitation(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Invitation UUID") @PathVariable UUID invitationId,
            @ActingMember MemberId actingMember) {

        MembersGroupId groupId = new MembersGroupId(id);
        InvitationId invId = new InvitationId(invitationId);
        membersGroupManagementService.rejectInvitation(groupId, invId, actingMember);
        return ResponseEntity.noContent().build();
    }

    private void requireOwnership(MembersGroupId groupId, MemberId actingMember) {
        MembersGroup group = membersGroupManagementService.getGroup(groupId);
        if (!group.isOwner(actingMember)) {
            throw new GroupOwnershipRequiredException(actingMember, groupId);
        }
    }

    private EntityModel<GroupSummaryResponse> buildGroupSummaryModel(MembersGroup group) {
        UUID groupId = group.getId().uuid();
        EntityModel<GroupSummaryResponse> model = EntityModel.of(new GroupSummaryResponse(group.getId(), group.getName()));
        klabisLinkTo(methodOn(MembersGroupController.class).getGroup(groupId, null))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }

    private GroupResponse toGroupResponse(MembersGroup group, UUID groupUuid, boolean requestingUserIsOwner) {
        Set<MemberId> ownerIds = group.getOwners();

        List<EntityModel<OwnerResponse>> ownerModels = ownerIds.stream()
                .map(ownerId -> buildOwnerModel(ownerId, groupUuid, requestingUserIsOwner, ownerIds.size()))
                .toList();

        List<EntityModel<MembersGroupMembershipResponse>> memberModels = group.getMembers().stream()
                .map(m -> buildMemberModel(m, groupUuid, requestingUserIsOwner, ownerIds))
                .toList();

        List<EntityModel<PendingInvitationResponse>> pendingInvitationModels = List.of();
        if (requestingUserIsOwner) {
            pendingInvitationModels = group.getPendingInvitations().stream()
                    .map(inv -> buildPendingInvitationModel(group, inv))
                    .toList();
        }

        return new GroupResponse(group.getId(), group.getName(), ownerModels, memberModels, pendingInvitationModels);
    }

    private EntityModel<OwnerResponse> buildOwnerModel(MemberId ownerId, UUID groupUuid, boolean requestingUserIsOwner, int ownerCount) {
        EntityModel<OwnerResponse> model = EntityModel.of(new OwnerResponse(ownerId.uuid()));
        model.add(Link.of("/api/members/" + ownerId.uuid(), "member"));
        if (requestingUserIsOwner && ownerCount > 1) {
            klabisLinkTo(methodOn(MembersGroupController.class).removeGroupOwner(groupUuid, ownerId.uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(MembersGroupController.class)
                                    .removeGroupOwner(groupUuid, ownerId.uuid(), null)))));
        }
        return model;
    }

    private EntityModel<MembersGroupMembershipResponse> buildMemberModel(
            GroupMembership membership, UUID groupUuid, boolean isOwner, Set<MemberId> ownerIds) {

        MemberId memberId = MemberId.fromUserId(membership.userId());
        MembersGroupMembershipResponse response = new MembersGroupMembershipResponse(memberId.uuid(), membership.joinedAt());

        EntityModel<MembersGroupMembershipResponse> model = EntityModel.of(response);
        model.add(Link.of("/api/members/" + memberId.uuid(), "member"));

        boolean memberIsOwner = ownerIds.contains(memberId);
        if (isOwner && !memberIsOwner) {
            klabisLinkTo(methodOn(MembersGroupController.class)
                    .removeGroupMember(groupUuid, memberId.uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(MembersGroupController.class)
                                    .removeGroupMember(groupUuid, memberId.uuid(), null)))));
        }

        return model;
    }

    private EntityModel<PendingInvitationResponse> buildPendingInvitationModel(MembersGroup group, Invitation invitation) {
        return InvitationModelBuilder.build(group, invitation);
    }

}

@MvcComponent
class GroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MembersGroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withRel("groups")));
        return model;
    }
}
