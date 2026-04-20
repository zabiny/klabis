package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.Invitation;
import com.klabis.common.usergroup.InvitationId;
import com.klabis.common.users.Authority;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.domain.FreeGroup;
import org.springframework.hateoas.server.ExposesResourceFor;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Groups", description = "Members group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.GROUPS_SCOPE})
@ExposesResourceFor(FreeGroup.class)
class FreeGroupController {

    private final FreeGroupManagementPort membersGroupManagementService;

    FreeGroupController(FreeGroupManagementPort membersGroupManagementService) {
        this.membersGroupManagementService = membersGroupManagementService;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create a members group")
    ResponseEntity<Void> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @ActingMember MemberId actingMember) {

        FreeGroup group = membersGroupManagementService.createGroup(request.name(), actingMember);

        return ResponseEntity.created(
                linkTo(methodOn(FreeGroupController.class).getGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List groups where authenticated member is a member")
    ResponseEntity<CollectionModel<EntityModel<GroupSummaryResponse>>> listGroups(
            @ActingMember MemberId actingMember) {

        List<FreeGroup> groups = membersGroupManagementService.listGroupsForMember(actingMember);
        List<EntityModel<GroupSummaryResponse>> items = groups.stream()
                .map(this::buildGroupSummaryModel)
                .toList();

        CollectionModel<EntityModel<GroupSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(FreeGroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class).createGroup(null, null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group details (owner or member only)")
    ResponseEntity<EntityModel<GroupResponse>> getGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        FreeGroup group = membersGroupManagementService.getGroup(groupId);

        boolean isOwner = group.isOwner(actingMember);
        boolean isMember = group.hasMember(actingMember);
        if (!isOwner && !isMember) {
            throw new InsufficientAuthorityException("Free group membership or ownership required");
        }

        GroupResponse response = toGroupResponse(group, id, isOwner);
        var model = entityModelWithDomain(response, group);

        klabisLinkTo(methodOn(FreeGroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withRel("collection")));

        return ResponseEntity.ok(model);
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Rename a group (owner only)")
    ResponseEntity<Void> updateGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody RenameGroupRequest request,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.renameGroup(groupId, request.name(), actingMember);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a group (owner only)")
    ResponseEntity<Void> deleteGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.deleteGroup(groupId, actingMember);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from group (owner only)")
    ResponseEntity<Void> removeGroupMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.removeMember(groupId, new MemberId(memberId), actingMember);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/owners", consumes = "application/json")
    @Operation(summary = "Add an owner to group (owner only)")
    ResponseEntity<Void> addGroupOwner(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddOwnerRequest request,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.addOwner(groupId, new MemberId(request.memberId()), actingMember);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/owners/{memberId}")
    @Operation(summary = "Remove an owner from group (owner only)")
    ResponseEntity<Void> removeGroupOwner(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Owner member UUID") @PathVariable UUID memberId,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.removeOwner(groupId, new MemberId(memberId), actingMember);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/invitations", consumes = "application/json",
            produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "Invite a member to a members group (owner only)")
    ResponseEntity<Void> inviteMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody InviteMemberRequest request,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.inviteMember(groupId, actingMember, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    @Operation(summary = "Cancel a pending invitation (owner only)")
    ResponseEntity<Void> cancelInvitation(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Invitation UUID") @PathVariable UUID invitationId,
            @RequestBody(required = false) CancelInvitationRequest request,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        InvitationId invId = new InvitationId(invitationId);
        String reason = request != null ? request.reason() : null;
        membersGroupManagementService.cancelInvitation(groupId, invId, actingMember, reason);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/invitations/{invitationId}/accept",
            produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    @Operation(summary = "Accept a pending invitation")
    ResponseEntity<Void> acceptInvitation(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Invitation UUID") @PathVariable UUID invitationId,
            @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
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

        FreeGroupId groupId = new FreeGroupId(id);
        InvitationId invId = new InvitationId(invitationId);
        membersGroupManagementService.rejectInvitation(groupId, invId, actingMember);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<GroupSummaryResponse> buildGroupSummaryModel(FreeGroup group) {
        UUID groupId = group.getId().uuid();
        EntityModel<GroupSummaryResponse> model = EntityModel.of(new GroupSummaryResponse(group.getId(), group.getName()));
        klabisLinkTo(methodOn(FreeGroupController.class).getGroup(groupId, null))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }

    private GroupResponse toGroupResponse(FreeGroup group, UUID groupUuid, boolean requestingUserIsOwner) {
        Set<MemberId> ownerIds = group.getOwners();

        List<EntityModel<OwnerResponse>> ownerModels = ownerIds.stream()
                .map(ownerId -> buildOwnerModel(ownerId, groupUuid, requestingUserIsOwner, ownerIds.size()))
                .toList();

        List<EntityModel<FreeGroupMembershipResponse>> memberModels = group.getMembers().stream()
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
        klabisLinkTo(methodOn(MemberController.class).getMember(ownerId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        if (requestingUserIsOwner && ownerCount > 1) {
            klabisLinkTo(methodOn(FreeGroupController.class).removeGroupOwner(groupUuid, ownerId.uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(FreeGroupController.class)
                                    .removeGroupOwner(groupUuid, ownerId.uuid(), null)))));
        }
        return model;
    }

    private EntityModel<FreeGroupMembershipResponse> buildMemberModel(
            GroupMembership membership, UUID groupUuid, boolean isOwner, Set<MemberId> ownerIds) {

        MemberId memberId = MemberId.fromUserId(membership.userId());
        FreeGroupMembershipResponse response = new FreeGroupMembershipResponse(memberId.uuid(), membership.joinedAt());

        EntityModel<FreeGroupMembershipResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(MemberController.class).getMember(memberId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);

        boolean memberIsOwner = ownerIds.contains(memberId);
        if (isOwner && !memberIsOwner) {
            klabisLinkTo(methodOn(FreeGroupController.class)
                    .removeGroupMember(groupUuid, memberId.uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(FreeGroupController.class)
                                    .removeGroupMember(groupUuid, memberId.uuid(), null)))));
        }

        return model;
    }

    private EntityModel<PendingInvitationResponse> buildPendingInvitationModel(FreeGroup group, Invitation invitation) {
        return InvitationModelBuilder.build(group, invitation);
    }

}

@MvcComponent
class FreeGroupDetailsPostprocessor extends ModelWithDomainPostprocessor<GroupResponse, FreeGroup> {

    @Override
    public void process(EntityModel<GroupResponse> dtoModel, FreeGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(FreeGroupController.class).getGroup(id, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel();
            if (isActingMemberOwner(group)) {
                selfLink = selfLink
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class).updateGroup(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class).deleteGroup(id, null)))
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class).addGroupOwner(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(FreeGroupController.class).inviteMember(id, null, null)));
            }
            dtoModel.add(selfLink);
        });
    }

    private boolean isActingMemberOwner(FreeGroup group) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof KlabisJwtAuthenticationToken token) {
            return token.getMemberIdUuid()
                    .map(MemberId::new)
                    .map(group::isOwner)
                    .orElse(false);
        }
        return false;
    }
}

@MvcComponent
class GroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(FreeGroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withRel("groups")));
        return model;
    }
}
