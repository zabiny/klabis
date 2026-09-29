package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalFormsOptionsDef;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.ActingUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.FamilyGroupId;
import com.klabis.members.familygroup.application.FamilyGroupManagementPort;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.infrastructure.restapi.*;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(FamilyGroup.class)
class FamilyGroupController implements FamilyGroupsApi {

    private final FamilyGroupManagementPort familyGroupManagementService;

    FamilyGroupController(FamilyGroupManagementPort familyGroupManagementService) {
        this.familyGroupManagementService = familyGroupManagementService;
    }

    @Override
    public ResponseEntity<Void> createFamilyGroup(CreateFamilyGroupRequest request) {

        FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                request.name(), new UserId(request.parent()));
        FamilyGroup group = familyGroupManagementService.createFamilyGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(FamilyGroupsApi.class).getFamilyGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<List<FamilyGroupSummaryResponse>> listFamilyGroups() {

        List<FamilyGroup> groups = familyGroupManagementService.listFamilyGroups();

        HalResponseContext.setDomainList(groups);
        return ResponseEntity.ok(groups.stream().map(this::toSummaryResponse).toList());
    }

    // The FamilyGroupResponse record is generated, but its parents/members arrays are
    // List<EntityModel<X>> and each item carries its own _links/_templates at runtime — children a
    // "member" link, parent rows a self link with a DELETE affordance when the caller may remove the
    // parent — which the generator cannot express on the payload. The controller assembles them.
    @Override
    public ResponseEntity<FamilyGroupResponse> getFamilyGroup(
            UUID id,
            @ActingUser CurrentUserData currentUser) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        FamilyGroup group = familyGroupManagementService.getFamilyGroup(groupId);

        boolean hasMembersManage = currentUser.hasAuthority(Authority.MEMBERS_MANAGE);
        boolean isMember = group.hasMember(currentUser.userId());

        if (!hasMembersManage && !isMember) {
            throw new InsufficientAuthorityException("MEMBERS:MANAGE or family group membership required");
        }

        HalResponseContext.setDomain(group);
        return ResponseEntity.ok(toFamilyGroupResponse(group, hasMembersManage));
    }

    @Override
    public ResponseEntity<Void> deleteFamilyGroup(UUID id) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.deleteFamilyGroup(groupId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> addFamilyGroupParent(UUID id, AddParentRequest request) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.addParent(groupId, new UserId(request.userId()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeFamilyGroupParent(UUID id, UUID userId) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        UserId parentToRemove = new UserId(userId);
        familyGroupManagementService.removeParent(groupId, parentToRemove);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> addFamilyGroupChild(UUID id, AddMemberRequest request) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.addChild(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeFamilyGroupChild(UUID id, UUID memberId) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.removeChild(groupId, new MemberId(memberId));
        return ResponseEntity.noContent().build();
    }

    private FamilyGroupSummaryResponse toSummaryResponse(FamilyGroup group) {
        return FamilyGroupSummaryResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .memberCount(group.getMembers().size())
                .build();
    }

    private FamilyGroupResponse toFamilyGroupResponse(FamilyGroup group, boolean hasMembersManage) {
        UUID groupUuid = group.getId().uuid();
        Set<UserId> parentIds = group.getParents();
        List<EntityModel<ParentResponse>> parentModels = parentIds.stream()
                .map(parentId -> {
                    EntityModel<ParentResponse> model = EntityModel.of(ParentResponseBuilder.builder().userId(parentId.uuid()).build());
                    if (hasMembersManage && parentIds.size() > 1) {
                        klabisLinkTo(methodOn(FamilyGroupsApi.class).removeFamilyGroupParent(groupUuid, parentId.uuid()))
                                .ifPresent(link -> model.add(link.withSelfRel()
                                        .andAffordances(klabisAfford(methodOn(FamilyGroupsApi.class)
                                                .removeFamilyGroupParent(groupUuid, parentId.uuid())))));
                    }
                    return model;
                })
                .toList();

        List<EntityModel<FamilyGroupMembershipResponse>> memberModels = group.getChildren().stream()
                .map(m -> buildChildModel(m, groupUuid, hasMembersManage))
                .toList();

        return FamilyGroupResponseBuilder.builder()
                .id(group.getId().uuid())
                .name(group.getName())
                .parents(parentModels)
                .members(memberModels)
                .build();
    }

    private EntityModel<FamilyGroupMembershipResponse> buildChildModel(GroupMembership<MemberId> membership, UUID groupUuid, boolean hasMembersManage) {
        MemberId memberId = membership.memberId();
        FamilyGroupMembershipResponse response = FamilyGroupMembershipResponseBuilder.builder()
                .memberId(memberId.uuid())
                .joinedAt(membership.joinedAt())
                .build();
        EntityModel<FamilyGroupMembershipResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(MembersApi.class).getMember(memberId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        if (hasMembersManage) {
            klabisLinkTo(methodOn(FamilyGroupsApi.class).removeFamilyGroupChild(groupUuid, memberId.uuid()))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(FamilyGroupsApi.class)
                                    .removeFamilyGroupChild(groupUuid, memberId.uuid())))));
        }
        return model;
    }
}

@MvcComponent
class FamilyGroupSummaryPostprocessor extends ModelWithDomainPostprocessor<FamilyGroupSummaryResponse, FamilyGroup> {

    @Override
    public void process(EntityModel<FamilyGroupSummaryResponse> dtoModel, FamilyGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(FamilyGroupsApi.class).getFamilyGroup(id, null))
                .ifPresent(link -> dtoModel.add(link.withSelfRel()));
    }
}

@MvcComponent
class FamilyGroupDetailsPostprocessor extends ModelWithDomainPostprocessor<FamilyGroupResponse, FamilyGroup> {

    @Override
    public void process(EntityModel<FamilyGroupResponse> dtoModel, FamilyGroup group) {
        UUID id = group.getId().uuid();
        // A parent need not be a club member, so the add-parent form is keyed on userId. The options
        // still come from listMemberOptions — offering non-member users is a later step, and their
        // UUIDs are the same either way.
        Map<String, HalFormsOptionsDef> parentUserIdOptions = Map.of("userId",
                HalFormsOptionsDef.remote(methodOn(MembersApi.class).listMemberOptions()));
        Map<String, HalFormsOptionsDef> childMemberIdOptions = Map.of("memberId",
                HalFormsOptionsDef.remote(methodOn(MembersApi.class).listMemberOptions()));
        klabisLinkTo(methodOn(FamilyGroupsApi.class).getFamilyGroup(id, null))
                .map(link -> link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(FamilyGroupsApi.class).deleteFamilyGroup(id)))
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(FamilyGroupsApi.class).addFamilyGroupParent(id, null), parentUserIdOptions))
                        .andAffordances(klabisAffordWithOptions(
                                methodOn(FamilyGroupsApi.class).addFamilyGroupChild(id, null), childMemberIdOptions)))
                .ifPresent(dtoModel::add);

        // klabisLinkTo omits this for callers without MEMBERS:MANAGE, which is the authority
        // listFamilyGroups requires — the same condition the controller used to check by hand.
        klabisLinkTo(methodOn(FamilyGroupsApi.class).listFamilyGroups())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

@MvcComponent
class FamilyGroupsRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(FamilyGroupsApi.class).listFamilyGroups())
                .ifPresent(link -> model.add(link.withRel("family-groups")));
        return model;
    }
}

// The self link itself is built by HalResponseBodyAdvice from the current request; this processor
// only contributes the create affordance, which stays authorization-sensitive via klabisAfford.
@MvcComponent
class FamilyGroupListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<FamilyGroupSummaryResponse>>> {

    @Override
    public CollectionModel<EntityModel<FamilyGroupSummaryResponse>> process(
            CollectionModel<EntityModel<FamilyGroupSummaryResponse>> model) {
        model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, selfLink -> (org.springframework.hateoas.Link) selfLink
                .andAffordances(klabisAffordWithOptions(
                        methodOn(FamilyGroupsApi.class).createFamilyGroup(null),
                        Map.of("parent", HalFormsOptionsDef.remote(methodOn(MembersApi.class).listMemberOptions())))));
        return model;
    }
}
