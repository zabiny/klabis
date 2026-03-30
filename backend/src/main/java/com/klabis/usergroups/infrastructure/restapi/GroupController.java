package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.GroupManagementPort;
import com.klabis.usergroups.domain.FreeGroup;
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
import java.util.UUID;

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

    GroupController(GroupManagementPort groupManagementService) {
        this.groupManagementService = groupManagementService;
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

        GroupResponse response = toGroupResponse(group);
        EntityModel<GroupResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(GroupController.class).getGroup(id, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(GroupController.class).renameGroup(id, null, null)))
                    .andAffordances(klabisAfford(methodOn(GroupController.class).deleteGroup(id, null)))
                    .andAffordances(klabisAfford(methodOn(GroupController.class).addMember(id, null, null)));
            model.add(selfLink);
        });

        klabisLinkTo(methodOn(GroupController.class).listGroups(null))
                .ifPresent(link -> model.add(link.withRel("collection")));

        return ResponseEntity.ok(model);
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Rename a group (owner only)")
    ResponseEntity<Void> renameGroup(
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
    ResponseEntity<Void> addMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMemberProfile(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        MemberId memberToAdd = new MemberId(request.memberId());
        groupManagementService.addMemberToGroup(groupId, memberToAdd, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from group (owner only)")
    ResponseEntity<Void> removeMember(
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

    private GroupResponse toGroupResponse(UserGroup group) {
        List<UUID> ownerIds = group.getOwners().stream().map(MemberId::uuid).toList();
        List<GroupMembershipResponse> memberList = group.getMembers().stream()
                .map(m -> new GroupMembershipResponse(m.memberId().uuid(), m.joinedAt()))
                .toList();
        return new GroupResponse(group.getId().uuid(), group.getName(), ownerIds, memberList);
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
