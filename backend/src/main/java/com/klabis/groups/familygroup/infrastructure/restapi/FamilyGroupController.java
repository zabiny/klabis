package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.common.users.Authority;
import com.klabis.groups.common.domain.GroupMembership;
import com.klabis.groups.familygroup.FamilyGroupId;
import com.klabis.groups.familygroup.application.FamilyGroupManagementPort;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberController;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "FamilyGroups", description = "Family group management API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.GROUPS_SCOPE})
@ExposesResourceFor(FamilyGroup.class)
class FamilyGroupController implements FamilyGroupsApi {

    private final FamilyGroupManagementPort familyGroupManagementService;

    FamilyGroupController(FamilyGroupManagementPort familyGroupManagementService) {
        this.familyGroupManagementService = familyGroupManagementService;
    }

    @Override
    public ResponseEntity<Void> createFamilyGroup(@RequestBody CreateFamilyGroupRequest request) {

        FamilyGroup.CreateFamilyGroup command = new FamilyGroup.CreateFamilyGroup(
                request.name(), new MemberId(request.parent()));
        FamilyGroup group = familyGroupManagementService.createFamilyGroup(command);

        return ResponseEntity.created(
                linkTo(methodOn(FamilyGroupController.class).getFamilyGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<Collection<FamilyGroupSummaryResponse>> listFamilyGroups() {

        List<FamilyGroup> groups = familyGroupManagementService.listFamilyGroups();

        HalResponseContext.setDomainList(groups);
        return ResponseEntity.ok(groups.stream().map(this::toSummaryResponse).toList());
    }

    // The FamilyGroupResponse record itself is hand-written because parents/members are
    // List<EntityModel<X>> — each item carries its own _links/_templates (a "member" link, plus a
    // self link with a DELETE affordance when the caller may remove it), which the generator cannot
    // express. The interface's payload type still comes from the spec.
    @Override
    public ResponseEntity<FamilyGroupResponse> getFamilyGroup(
            UUID id,
            @ActingUser CurrentUserData currentUser) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        FamilyGroup group = familyGroupManagementService.getFamilyGroup(groupId);

        boolean hasMembersManage = currentUser.hasAuthority(Authority.MEMBERS_MANAGE);
        boolean isMember = currentUser.isMemberOf(group::hasMember);

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
    public ResponseEntity<Void> addFamilyGroupParent(UUID id, @RequestBody AddMemberRequest request) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        familyGroupManagementService.addParent(groupId, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeFamilyGroupParent(UUID id, UUID memberId) {

        FamilyGroupId groupId = new FamilyGroupId(id);
        MemberId parentToRemove = new MemberId(memberId);
        familyGroupManagementService.removeParent(groupId, parentToRemove);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> addFamilyGroupChild(UUID id, @RequestBody AddMemberRequest request) {

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
        return new FamilyGroupSummaryResponse(group.getId(), group.getName(), group.getMembers().size());
    }

    private FamilyGroupResponse toFamilyGroupResponse(FamilyGroup group, boolean hasMembersManage) {
        UUID groupUuid = group.getId().uuid();
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
        MemberId memberId = membership.memberId();
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
class FamilyGroupSummaryPostprocessor extends ModelWithDomainPostprocessor<FamilyGroupSummaryResponse, FamilyGroup> {

    @Override
    public void process(EntityModel<FamilyGroupSummaryResponse> dtoModel, FamilyGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(FamilyGroupController.class).getFamilyGroup(id, null))
                .ifPresent(link -> dtoModel.add(link.withSelfRel()));
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

        // klabisLinkTo omits this for callers without MEMBERS:MANAGE, which is the authority
        // listFamilyGroups requires — the same condition the controller used to check by hand.
        klabisLinkTo(methodOn(FamilyGroupController.class).listFamilyGroups())
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
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

// The self link itself is built by HalResponseBodyAdvice from the current request; this processor
// only contributes the create affordance, which stays authorization-sensitive via klabisAfford.
@MvcComponent
class FamilyGroupListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<FamilyGroupSummaryResponse>>> {

    @Override
    public CollectionModel<EntityModel<FamilyGroupSummaryResponse>> process(
            CollectionModel<EntityModel<FamilyGroupSummaryResponse>> model) {
        model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, selfLink -> (org.springframework.hateoas.Link) selfLink
                .andAffordances(klabisAfford(methodOn(FamilyGroupController.class).createFamilyGroup(null))));
        return model;
    }
}
