package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.GroupManagementPort;
import com.klabis.usergroups.domain.AgeRange;
import com.klabis.usergroups.domain.GroupMembership;
import com.klabis.usergroups.domain.TrainingGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.MediaTypes;
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
@RequestMapping(value = "/api/training-groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "TrainingGroups", description = "Training group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class TrainingGroupController {

    private final GroupManagementPort groupManagementService;

    TrainingGroupController(GroupManagementPort groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create a training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> createTrainingGroup(
            @Valid @RequestBody CreateTrainingGroupRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        AgeRange ageRange = new AgeRange(request.minAge(), request.maxAge());
        TrainingGroup.CreateTrainingGroup command = new TrainingGroup.CreateTrainingGroup(
                request.name(), currentUser.memberId(), ageRange);
        TrainingGroup group = groupManagementService.createTrainingGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(TrainingGroupController.class).getTrainingGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @GetMapping
    @HasAuthority(Authority.GROUPS_TRAINING)
    @Operation(summary = "List all training groups (requires GROUPS:TRAINING)")
    ResponseEntity<CollectionModel<EntityModel<TrainingGroupSummaryResponse>>> listTrainingGroups(
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        List<TrainingGroup> groups = groupManagementService.listTrainingGroups();
        List<EntityModel<TrainingGroupSummaryResponse>> items = groups.stream()
                .map(this::buildTrainingGroupSummaryModel)
                .toList();

        CollectionModel<EntityModel<TrainingGroupSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(TrainingGroupController.class).listTrainingGroups(null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).createTrainingGroup(null, null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @HasAuthority(Authority.MEMBERS_READ)
    @Operation(summary = "Get training group details")
    ResponseEntity<EntityModel<TrainingGroupResponse>> getTrainingGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @CurrentUser CurrentUserData currentUser) {

        UserGroupId groupId = new UserGroupId(id);
        TrainingGroup group = groupManagementService.getTrainingGroup(groupId);

        EntityModel<TrainingGroupResponse> model;
        if (currentUser.hasAuthority(Authority.GROUPS_TRAINING)) {
            boolean isOwner = currentUser.isMember() && group.isOwner(currentUser.memberId());
            model = buildFullGroupModel(group, id, isOwner);
        } else {
            model = buildLimitedGroupModel(group, id);
        }

        klabisLinkTo(methodOn(TrainingGroupController.class).listTrainingGroups(null))
                .ifPresent(link -> model.add(link.withRel("collection")));

        return ResponseEntity.ok(model);
    }

    private EntityModel<TrainingGroupResponse> buildFullGroupModel(TrainingGroup group, UUID groupUuid, boolean isOwner) {
        TrainingGroupResponse response = toTrainingGroupResponse(group, groupUuid, isOwner);
        EntityModel<TrainingGroupResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(TrainingGroupController.class).getTrainingGroup(groupUuid, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel();
            if (isOwner) {
                selfLink = selfLink
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).updateTrainingGroup(groupUuid, null, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).updateAgeRange(groupUuid, null, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).deleteTrainingGroup(groupUuid, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).addTrainingGroupMember(groupUuid, null, null)))
                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class).addTrainingGroupOwner(groupUuid, null, null)));
            }
            model.add(selfLink);
        });

        return model;
    }

    private EntityModel<TrainingGroupResponse> buildLimitedGroupModel(TrainingGroup group, UUID groupUuid) {
        List<EntityModel<OwnerResponse>> ownerModels = group.getOwners().stream()
                .map(ownerId -> {
                    EntityModel<OwnerResponse> model = EntityModel.of(new OwnerResponse(ownerId.uuid()));
                    model.add(Link.of("/api/members/" + ownerId.uuid(), "member"));
                    return model;
                })
                .toList();

        TrainingGroupResponse response = new TrainingGroupResponse(
                group.getId(), group.getName(), null, null, ownerModels, null);
        EntityModel<TrainingGroupResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(TrainingGroupController.class).getTrainingGroup(groupUuid, null))
                .ifPresent(link -> model.add(link.withSelfRel()));

        return model;
    }

    @PatchMapping(value = "/{id}", consumes = "application/json")
    @Operation(summary = "Rename a training group (owner only)")
    ResponseEntity<Void> updateTrainingGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody RenameGroupRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.renameGroup(groupId, request.name(), currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{id}/age-range", consumes = "application/json")
    @Operation(summary = "Update training group age range (owner only)")
    ResponseEntity<Void> updateAgeRange(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateAgeRangeRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        AgeRange newAgeRange = new AgeRange(request.minAge(), request.maxAge());
        groupManagementService.updateTrainingGroupAgeRange(groupId, newAgeRange, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a training group (requires GROUPS:TRAINING)")
    ResponseEntity<Void> deleteTrainingGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.deleteGroup(groupId, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/members", consumes = "application/json")
    @Operation(summary = "Add a member to training group (owner only)")
    ResponseEntity<Void> addTrainingGroupMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.addMemberToGroup(groupId, request.memberId(), currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove a member from training group (owner only)")
    ResponseEntity<Void> removeTrainingGroupMember(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Member UUID") @PathVariable UUID memberId,
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        MemberId memberToRemove = new MemberId(memberId);
        groupManagementService.removeMemberFromGroup(groupId, memberToRemove, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/owners", consumes = "application/json")
    @Operation(summary = "Add an owner to training group (owner only)")
    ResponseEntity<Void> addTrainingGroupOwner(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddOwnerRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.addOwnerToGroup(groupId, request.toMemberId(), currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/owners/{memberId}")
    @Operation(summary = "Remove an owner from training group (owner only)")
    ResponseEntity<Void> removeTrainingGroupOwner(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Owner member UUID") @PathVariable UUID memberId,
            @CurrentUser CurrentUserData currentUser) {

        requireTrainingAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        MemberId ownerToRemove = new MemberId(memberId);
        groupManagementService.removeOwnerFromGroup(groupId, ownerToRemove, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    private EntityModel<TrainingGroupSummaryResponse> buildTrainingGroupSummaryModel(TrainingGroup group) {
        UUID groupId = group.getId().uuid();
        TrainingGroupSummaryResponse response = new TrainingGroupSummaryResponse(
                group.getId(), group.getName(),
                group.getAgeRange().minAge(), group.getAgeRange().maxAge(),
                group.getMembers().size());
        EntityModel<TrainingGroupSummaryResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(TrainingGroupController.class).getTrainingGroup(groupId, null))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }

    private TrainingGroupResponse toTrainingGroupResponse(TrainingGroup group, UUID groupUuid, boolean isOwner) {
        Set<MemberId> ownerIds = group.getOwners();

        List<EntityModel<OwnerResponse>> ownerModels = ownerIds.stream()
                .map(ownerId -> {
                    EntityModel<OwnerResponse> model = EntityModel.of(new OwnerResponse(ownerId.uuid()));
                    model.add(Link.of("/api/members/" + ownerId.uuid(), "member"));
                    if (isOwner && ownerIds.size() > 1) {
                        klabisLinkTo(methodOn(TrainingGroupController.class).removeTrainingGroupOwner(groupUuid, ownerId.uuid(), null))
                                .ifPresent(link -> model.add(link.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(TrainingGroupController.class)
                                                .removeTrainingGroupOwner(groupUuid, ownerId.uuid(), null)))));
                    }
                    return model;
                })
                .toList();

        List<EntityModel<GroupMembershipResponse>> memberModels = group.getMembers().stream()
                .map(m -> buildMemberModel(m, groupUuid, isOwner, ownerIds))
                .toList();

        return new TrainingGroupResponse(
                group.getId(), group.getName(),
                group.getAgeRange().minAge(), group.getAgeRange().maxAge(),
                ownerModels, memberModels);
    }

    private EntityModel<GroupMembershipResponse> buildMemberModel(
            GroupMembership membership, UUID groupUuid, boolean isOwner, Set<MemberId> ownerIds) {

        GroupMembershipResponse response = new GroupMembershipResponse(
                membership.memberId().uuid(), membership.joinedAt());
        EntityModel<GroupMembershipResponse> model = EntityModel.of(response);
        model.add(Link.of("/api/members/" + membership.memberId().uuid(), "member"));

        boolean memberIsOwner = ownerIds.contains(membership.memberId());
        if (isOwner && !memberIsOwner) {
            klabisLinkTo(methodOn(TrainingGroupController.class)
                    .removeTrainingGroupMember(groupUuid, membership.memberId().uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(TrainingGroupController.class)
                                    .removeTrainingGroupMember(groupUuid, membership.memberId().uuid(), null)))));
        }

        return model;
    }

    private void requireTrainingAuthority(CurrentUserData currentUser) {
        if (!currentUser.hasAuthority(Authority.GROUPS_TRAINING)) {
            throw new InsufficientAuthorityException("GROUPS:TRAINING");
        }
    }
}

@MvcComponent
class TrainingGroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(TrainingGroupController.class).listTrainingGroups(null))
                .ifPresent(link -> model.add(link.withRel("training-groups")));
        return model;
    }
}
