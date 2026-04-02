package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.GroupManagementPort;
import com.klabis.usergroups.domain.FamilyGroup;
import com.klabis.usergroups.domain.GroupMembership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import java.util.stream.Collectors;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/family-groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "FamilyGroups", description = "Family group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class FamilyGroupController {

    private final GroupManagementPort groupManagementService;

    FamilyGroupController(GroupManagementPort groupManagementService) {
        this.groupManagementService = groupManagementService;
    }

    record CreateFamilyGroupRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull List<UUID> memberIds
    ) {
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create a family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> createFamilyGroup(
            @Valid @RequestBody CreateFamilyGroupRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        Set<MemberId> initialMembers = request.memberIds().stream()
                .map(MemberId::new)
                .collect(Collectors.toSet());

        FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                request.name(), currentUser.memberId(), initialMembers);

        FamilyGroup group = groupManagementService.createFamilyGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(FamilyGroupController.class).getFamilyGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List all family groups (requires MEMBERS:MANAGE)")
    ResponseEntity<CollectionModel<EntityModel<FamilyGroupSummaryResponse>>> listFamilyGroups(
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        List<FamilyGroup> groups = groupManagementService.listFamilyGroups();
        List<EntityModel<FamilyGroupSummaryResponse>> items = groups.stream()
                .map(this::buildFamilyGroupSummaryModel)
                .toList();

        CollectionModel<EntityModel<FamilyGroupSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(FamilyGroupController.class).listFamilyGroups(null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).createFamilyGroup(null, null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get family group details (requires MEMBERS:MANAGE)")
    ResponseEntity<EntityModel<FamilyGroupResponse>> getFamilyGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        FamilyGroup group = groupManagementService.getFamilyGroup(groupId);
        boolean requestingUserIsOwner = group.isOwner(currentUser.memberId());

        FamilyGroupResponse response = toFamilyGroupResponse(group, id, requestingUserIsOwner);
        EntityModel<FamilyGroupResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(FamilyGroupController.class).getFamilyGroup(id, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).deleteFamilyGroup(id, null)));
            if (requestingUserIsOwner) {
                selfLink = selfLink
                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).addFamilyGroupOwner(id, null, null)));
            }
            model.add(selfLink);
        });

        klabisLinkTo(methodOn(FamilyGroupController.class).listFamilyGroups(null))
                .ifPresent(link -> model.add(link.withRel("collection")));

        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> deleteFamilyGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.deleteFamilyGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/owners", consumes = "application/json")
    @Operation(summary = "Add an owner to family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> addFamilyGroupOwner(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddOwnerRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        groupManagementService.addOwnerToGroup(groupId, request.toMemberId(), currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/owners/{memberId}")
    @Operation(summary = "Remove an owner from family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> removeFamilyGroupOwner(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Owner member UUID") @PathVariable UUID memberId,
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        UserGroupId groupId = new UserGroupId(id);
        MemberId ownerToRemove = new MemberId(memberId);
        groupManagementService.removeOwnerFromGroup(groupId, ownerToRemove, currentUser.memberId());
        return ResponseEntity.noContent().build();
    }

    private EntityModel<FamilyGroupSummaryResponse> buildFamilyGroupSummaryModel(FamilyGroup group) {
        UUID groupId = group.getId().uuid();
        FamilyGroupSummaryResponse response = new FamilyGroupSummaryResponse(
                groupId, group.getName(), group.getMembers().size());
        EntityModel<FamilyGroupSummaryResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(FamilyGroupController.class).getFamilyGroup(groupId, null))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }

    private FamilyGroupResponse toFamilyGroupResponse(FamilyGroup group, UUID groupUuid, boolean requestingUserIsOwner) {
        Set<MemberId> ownerIds = group.getOwners();
        List<EntityModel<OwnerResponse>> ownerModels = ownerIds.stream()
                .map(ownerId -> {
                    EntityModel<OwnerResponse> model = EntityModel.of(new OwnerResponse(ownerId.uuid()));
                    model.add(Link.of("/api/members/" + ownerId.uuid(), "member"));
                    if (requestingUserIsOwner && ownerIds.size() > 1) {
                        klabisLinkTo(methodOn(FamilyGroupController.class).removeFamilyGroupOwner(groupUuid, ownerId.uuid(), null))
                                .ifPresent(link -> model.add(link.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class)
                                                .removeFamilyGroupOwner(groupUuid, ownerId.uuid(), null)))));
                    }
                    return model;
                })
                .toList();

        List<EntityModel<GroupMembershipResponse>> memberModels = group.getMembers().stream()
                .map(m -> buildMemberModel(m, groupUuid))
                .toList();

        return new FamilyGroupResponse(group.getId().uuid(), group.getName(), ownerModels, memberModels);
    }

    private EntityModel<GroupMembershipResponse> buildMemberModel(GroupMembership membership, UUID groupUuid) {
        GroupMembershipResponse response = new GroupMembershipResponse(
                membership.memberId().uuid(), membership.joinedAt());
        EntityModel<GroupMembershipResponse> model = EntityModel.of(response);
        model.add(Link.of("/api/members/" + membership.memberId().uuid(), "member"));
        return model;
    }

    private void requireMembersManageAuthority(CurrentUserData currentUser) {
        if (!currentUser.hasAuthority(Authority.MEMBERS_MANAGE)) {
            throw new InsufficientAuthorityException("MEMBERS:MANAGE");
        }
    }
}

record FamilyGroupSummaryResponse(UUID id, String name, int memberCount) {
}

record FamilyGroupResponse(UUID id, String name,
                            List<EntityModel<OwnerResponse>> owners,
                            List<EntityModel<GroupMembershipResponse>> members) {
}

@MvcComponent
class FamilyGroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(FamilyGroupController.class).listFamilyGroups(null))
                .ifPresent(link -> model.add(link.withRel("family-groups")));
        return model;
    }
}
