package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.groups.familygroup.FamilyGroupId;
import com.klabis.groups.familygroup.application.FamilyGroupManagementPort;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import org.springframework.hateoas.server.ExposesResourceFor;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
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
@RequestMapping(value = "/api/family-groups", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "FamilyGroups", description = "Family group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.GROUPS_SCOPE})
@ExposesResourceFor(FamilyGroup.class)
class FamilyGroupController {

    private final FamilyGroupManagementPort familyGroupManagementService;

    FamilyGroupController(FamilyGroupManagementPort familyGroupManagementService) {
        this.familyGroupManagementService = familyGroupManagementService;
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Create a family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> createFamilyGroup(
            @Valid @RequestBody CreateFamilyGroupRequest request) {

        FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                request.name(), new MemberId(request.parent()));
        FamilyGroup group = familyGroupManagementService.createFamilyGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(FamilyGroupController.class).getFamilyGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @GetMapping
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "List all family groups (requires MEMBERS:MANAGE)")
    ResponseEntity<CollectionModel<EntityModel<FamilyGroupSummaryResponse>>> listFamilyGroups() {

        List<FamilyGroup> groups = familyGroupManagementService.listFamilyGroups();
        List<EntityModel<FamilyGroupSummaryResponse>> items = groups.stream()
                .map(this::buildFamilyGroupSummaryModel)
                .toList();

        CollectionModel<EntityModel<FamilyGroupSummaryResponse>> model = CollectionModel.of(items);
        klabisLinkTo(methodOn(FamilyGroupController.class).listFamilyGroups())
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).createFamilyGroup(null)))));

        return ResponseEntity.ok(model);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get family group details")
    ResponseEntity<EntityModel<FamilyGroupResponse>> getFamilyGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @ActingUser CurrentUserData currentUser) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        FamilyGroup group = familyGroupManagementService.getFamilyGroup(groupId);

        boolean hasMembersManage = currentUser.hasAuthority(Authority.MEMBERS_MANAGE);
        boolean isMember = currentUser.isMemberOf(group::hasMember);

        if (!hasMembersManage && !isMember) {
            throw new InsufficientAuthorityException("MEMBERS:MANAGE or family group membership required");
        }

        FamilyGroupResponse response = toFamilyGroupResponse(group, id, hasMembersManage);
        var model = entityModelWithDomain(response, group);

        if (hasMembersManage) {
            klabisLinkTo(methodOn(FamilyGroupController.class).listFamilyGroups())
                    .ifPresent(link -> model.add(link.withRel("collection")));
        }

        return ResponseEntity.ok(model);
    }

    @DeleteMapping("/{id}")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Delete a family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> deleteFamilyGroup(
            @Parameter(description = "Group UUID") @PathVariable UUID id) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.deleteFamilyGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/parents", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Add a parent to family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> addFamilyGroupParent(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.addParent(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/parents/{memberId}")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Remove a parent from family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> removeFamilyGroupParent(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Parent member UUID") @PathVariable UUID memberId) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        MemberId parentToRemove = new MemberId(memberId);
        familyGroupManagementService.removeParent(groupId, parentToRemove);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/children", consumes = "application/json")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Add a child to family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> addFamilyGroupChild(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.addChild(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/children/{memberId}")
    @HasAuthority(Authority.MEMBERS_MANAGE)
    @Operation(summary = "Remove a child from family group (requires MEMBERS:MANAGE)")
    ResponseEntity<Void> removeFamilyGroupChild(
            @Parameter(description = "Group UUID") @PathVariable UUID id,
            @Parameter(description = "Child member UUID") @PathVariable UUID memberId) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.removeChild(groupId, new MemberId(memberId));
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
                    klabisLinkTo(methodOn(MemberController.class).getMember(parentId.uuid(), null))
                            .map(link -> link.withRel("member"))
                            .ifPresent(model::add);
                    if (hasMembersManage && parentIds.size() > 1) {
                        klabisLinkTo(methodOn(FamilyGroupController.class).removeFamilyGroupParent(groupUuid, parentId.uuid()))
                                .ifPresent(link -> model.add(link.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class)
                                                .removeFamilyGroupParent(groupUuid, parentId.uuid())))));
                    }
                    return model;
                })
                .toList();

        List<EntityModel<FamilyGroupMembershipResponse>> memberModels = group.getChildren().stream()
                .map(m -> buildChildModel(m, groupUuid, hasMembersManage))
                .toList();

        return new FamilyGroupResponse(group.getId(), group.getName(), parentModels, memberModels);
    }

    private EntityModel<FamilyGroupMembershipResponse> buildChildModel(GroupMembership membership, UUID groupUuid, boolean hasMembersManage) {
        MemberId memberId = MemberId.fromUserId(membership.userId());
        FamilyGroupMembershipResponse response = new FamilyGroupMembershipResponse(memberId.uuid(), membership.joinedAt());
        EntityModel<FamilyGroupMembershipResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(MemberController.class).getMember(memberId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        if (hasMembersManage) {
            klabisLinkTo(methodOn(FamilyGroupController.class).removeFamilyGroupChild(groupUuid, memberId.uuid()))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(FamilyGroupController.class)
                                    .removeFamilyGroupChild(groupUuid, memberId.uuid())))));
        }
        return model;
    }
}

@MvcComponent
class FamilyGroupDetailsPostprocessor extends ModelWithDomainPostprocessor<FamilyGroupResponse, FamilyGroup> {

    @Override
    public void process(EntityModel<FamilyGroupResponse> dtoModel, FamilyGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(FamilyGroupController.class).getFamilyGroup(id, null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).deleteFamilyGroup(id)))
                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).addFamilyGroupParent(id, null)))
                        .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).addFamilyGroupChild(id, null))))
                .ifPresent(dtoModel::add);
    }
}

@MvcComponent
class FamilyGroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(FamilyGroupController.class).listFamilyGroups())
                .ifPresent(link -> model.add(link.withRel("family-groups")));
        return model;
    }
}
