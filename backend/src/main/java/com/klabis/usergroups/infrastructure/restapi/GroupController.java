package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.GroupManagementPort;
import com.klabis.usergroups.domain.FreeGroup;
import com.klabis.usergroups.domain.GroupMembership;
import com.klabis.usergroups.domain.UserGroup;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Groups", description = "User group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class GroupController {

    private final GroupManagementPort groupManagementService;
    private final Members members;

    GroupController(GroupManagementPort groupManagementService, Members members) {
        this.groupManagementService = groupManagementService;
        this.members = members;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create a free group")
    ResponseEntity<Void> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        FreeGroup.CreateFreeGroup command = new FreeGroup.CreateFreeGroup(request.name(), currentUser.memberId());
        UserGroup group = groupManagementService.createFreeGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(GroupController.class).getGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List groups where authenticated member is a member")
    ResponseEntity<CollectionModel<EntityModel<GroupSummaryResponse>>> listGroups(
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        List<UserGroup> groups = groupManagementService.listGroupsForMember(currentUser.memberId());
        List<EntityModel<GroupSummaryResponse>> items = groups.stream()
                .map(g -> buildGroupSummaryModel(g))
                .toList();

        CollectionModel<EntityModel<GroupSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(GroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(GroupController.class).createGroup(null, null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group details")
    ResponseEntity<EntityModel<GroupResponse>> getGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @CurrentUser CurrentUserData currentUser) {

        UserGroupId groupId = new UserGroupId(id);
        UserGroup group = groupManagementService.getGroup(groupId);

        MemberId requestingMember = currentUser != null ? currentUser.memberId() : null;
        boolean isOwner = requestingMember != null && group.isOwner(requestingMember);

        GroupResponse response = toGroupResponse(group, id, isOwner);
        EntityModel<GroupResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(GroupController.class).getGroup(id, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel();
            if (isOwner) {
                selfLink = selfLink
                        .andAffordances(klabisAfford(methodOn(GroupController.class).updateGroup(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(GroupController.class).deleteGroup(id, null)))
                        .andAffordances(klabisAfford(methodOn(GroupController.class).addGroupMember(id, null, null)));
            }
            model.add(selfLink);
        });

        klabisLinkTo(methodOn(GroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withRel("collection")));

        return ResponseEntity.ok(model);
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Rename a group (owner only)")
    ResponseEntity<Void> updateGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody RenameGroupRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.renameGroup(groupId, request.name(), currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a group (owner only)")
    ResponseEntity<Void> deleteGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.deleteGroup(groupId, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/members", consumes = "application/json")
    @Operation(summary = "Add a member to group (owner only)")
    ResponseEntity<Void> addGroupMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.addMemberToGroup(groupId, request.memberId(), currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from group (owner only)")
    ResponseEntity<Void> removeGroupMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        MemberId memberToRemove = new MemberId(memberId);
        groupManagementService.removeMemberFromGroup(groupId, memberToRemove, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    private EntityModel<GroupSummaryResponse> buildGroupSummaryModel(UserGroup group) {
        UUID groupId = group.getId().uuid();
        EntityModel<GroupSummaryResponse> model = EntityModel.of(toGroupSummaryResponse(group));
        klabisLinkTo(methodOn(GroupController.class).getGroup(groupId, null))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }

    private GroupSummaryResponse toGroupSummaryResponse(UserGroup group) {
        return new GroupSummaryResponse(group.getId().uuid(), group.getName());
    }

    private GroupResponse toGroupResponse(UserGroup group, UUID groupUuid, boolean requestingUserIsOwner) {
        Set<MemberId> ownerIdSet = group.getOwners();
        List<MemberId> ownerIds = ownerIdSet.stream().toList();
        List<MemberId> memberIds = group.getMembers().stream().map(GroupMembership::memberId).toList();

        List<MemberId> allIds = ownerIds.stream()
                .filter(id -> !memberIds.contains(id))
                .collect(Collectors.toList());
        allIds.addAll(memberIds);

        Map<MemberId, MemberDto> memberDtoMap = members.findByIds(allIds);

        List<OwnerResponse> ownerResponses = ownerIds.stream()
                .map(id -> toOwnerResponse(id, memberDtoMap.get(id)))
                .toList();

        List<EntityModel<GroupMembershipResponse>> memberResponses = group.getMembers().stream()
                .map(m -> buildMemberModel(m, memberDtoMap.get(m.memberId()), groupUuid, requestingUserIsOwner, ownerIdSet))
                .toList();

        return new GroupResponse(group.getId().uuid(), group.getName(), ownerResponses, memberResponses);
    }

    private OwnerResponse toOwnerResponse(MemberId ownerId, MemberDto dto) {
        if (dto == null) {
            return new OwnerResponse(ownerId.uuid(), null, null, null);
        }
        return new OwnerResponse(dto.memberId(), dto.firstName(), dto.lastName(), dto.registrationNumber());
    }

    private EntityModel<GroupMembershipResponse> buildMemberModel(
            GroupMembership membership, MemberDto dto, UUID groupUuid, boolean isOwner, Set<MemberId> ownerIds) {

        GroupMembershipResponse response;
        if (dto == null) {
            response = new GroupMembershipResponse(membership.memberId().uuid(), null, null, null, membership.joinedAt());
        } else {
            response = new GroupMembershipResponse(
                    membership.memberId().uuid(),
                    dto.firstName(),
                    dto.lastName(),
                    dto.registrationNumber(),
                    membership.joinedAt());
        }

        EntityModel<GroupMembershipResponse> model = EntityModel.of(response);

        boolean memberIsOwner = ownerIds.contains(membership.memberId());
        if (isOwner && !memberIsOwner) {
            klabisLinkTo(methodOn(GroupController.class)
                    .removeGroupMember(groupUuid, membership.memberId().uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(GroupController.class)
                                    .removeGroupMember(groupUuid, membership.memberId().uuid(), null)))));
        }

        return model;
    }

    private void requireMemberProfile(CurrentUserData currentUser) {
        if (!currentUser.isMember()) {
            throw new MemberProfileRequiredException();
        }
    }
}

@MvcComponent
class GroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(GroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withRel("groups")));
        return model;
    }
}
