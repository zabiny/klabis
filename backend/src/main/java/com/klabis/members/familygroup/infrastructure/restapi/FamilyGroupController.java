package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.members.CurrentUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.application.FamilyGroupManagementPort;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
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

    private final FamilyGroupManagementPort familyGroupManagementService;

    FamilyGroupController(FamilyGroupManagementPort familyGroupManagementService) {
        this.familyGroupManagementService = familyGroupManagementService;
    }

    record CreateFamilyGroupRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull @Size(min = 1) List<UUID> parentIds,
            @NotNull List<UUID> memberIds
    ) {
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Create a family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> createFamilyGroup(
            @Valid @RequestBody CreateFamilyGroupRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        Set<MemberId> parents = request.parentIds().stream()
                .map(MemberId::new)
                .collect(Collectors.toSet());

        Set<MemberId> initialMembers = request.memberIds().stream()
                .map(MemberId::new)
                .collect(Collectors.toSet());

        FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                request.name(), parents, initialMembers);

        FamilyGroup group = familyGroupManagementService.createFamilyGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(FamilyGroupController.class).getFamilyGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @GetMapping
    @Operation(summary = "List all family groups (requires MEMBERS:MANAGE)")
    ResponseEntity<CollectionModel<EntityModel<FamilyGroupSummaryResponse>>> listFamilyGroups(
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        List<FamilyGroup> groups = familyGroupManagementService.listFamilyGroups();
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

        FamilyGroupId groupId = new FamilyGroupId(id);
        FamilyGroup group = familyGroupManagementService.getFamilyGroup(groupId);
        boolean hasMembersManage = currentUser.hasAuthority(Authority.MEMBERS_MANAGE);

        FamilyGroupResponse response = toFamilyGroupResponse(group, id, hasMembersManage);
        EntityModel<FamilyGroupResponse> model = EntityModel.of(response);

        klabisLinkTo(methodOn(FamilyGroupController.class).getFamilyGroup(id, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).deleteFamilyGroup(id, null)));
            if (hasMembersManage) {
                selfLink = selfLink
                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).addFamilyGroupParent(id, null, null)));
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

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.deleteFamilyGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/parents", consumes = "application/json")
    @Operation(summary = "Add a parent to family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> addFamilyGroupParent(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddParentRequest request,
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.addParent(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/parents/{memberId}")
    @Operation(summary = "Remove a parent from family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> removeFamilyGroupParent(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Parent member UUID") @PathVariable UUID memberId,
            @CurrentUser CurrentUserData currentUser) {

        requireMembersManageAuthority(currentUser);

        FamilyGroupId groupId = new FamilyGroupId(id);
        MemberId parentToRemove = new MemberId(memberId);
        familyGroupManagementService.removeParent(groupId, parentToRemove);
        return ResponseEntity.noContent().build();
    }

    private EntityModel<FamilyGroupSummaryResponse> buildFamilyGroupSummaryModel(FamilyGroup group) {
        UUID groupId = group.getId().uuid();
        FamilyGroupSummaryResponse response = new FamilyGroupSummaryResponse(
                group.getId(), group.getName(), group.getMembers().size());
        EntityModel<FamilyGroupSummaryResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(FamilyGroupController.class).getFamilyGroup(groupId, null))
                .ifPresent(link -> model.add(link.withSelfRel()));
        return model;
    }

    private FamilyGroupResponse toFamilyGroupResponse(FamilyGroup group, UUID groupUuid, boolean hasMembersManage) {
        Set<MemberId> parentIds = group.getParents();
        List<EntityModel<ParentResponse>> parentModels = parentIds.stream()
                .map(parentId -> {
                    EntityModel<ParentResponse> model = EntityModel.of(new ParentResponse(parentId.uuid()));
                    model.add(Link.of("/api/members/" + parentId.uuid(), "member"));
                    if (hasMembersManage && parentIds.size() > 1) {
                        klabisLinkTo(methodOn(FamilyGroupController.class).removeFamilyGroupParent(groupUuid, parentId.uuid(), null))
                                .ifPresent(link -> model.add(link.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class)
                                                .removeFamilyGroupParent(groupUuid, parentId.uuid(), null)))));
                    }
                    return model;
                })
                .toList();

        List<EntityModel<FamilyGroupMembershipResponse>> memberModels = group.getMembers().stream()
                .map(m -> buildMemberModel(m, groupUuid))
                .toList();

        return new FamilyGroupResponse(group.getId(), group.getName(), parentModels, memberModels);
    }

    private EntityModel<FamilyGroupMembershipResponse> buildMemberModel(GroupMembership membership, UUID groupUuid) {
        MemberId memberId = MemberId.fromUserId(membership.userId());
        FamilyGroupMembershipResponse response = new FamilyGroupMembershipResponse(memberId.uuid(), membership.joinedAt());
        EntityModel<FamilyGroupMembershipResponse> model = EntityModel.of(response);
        model.add(Link.of("/api/members/" + memberId.uuid(), "member"));
        return model;
    }

    private void requireMembersManageAuthority(CurrentUserData currentUser) {
        if (!currentUser.hasAuthority(Authority.MEMBERS_MANAGE)) {
            throw new InsufficientAuthorityException("MEMBERS:MANAGE");
        }
    }
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
