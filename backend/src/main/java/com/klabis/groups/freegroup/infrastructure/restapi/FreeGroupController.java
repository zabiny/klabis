package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.exceptions.InsufficientAuthorityException;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.groups.common.domain.GroupMembership;
import com.klabis.groups.freegroup.domain.Invitation;
import com.klabis.groups.freegroup.domain.InvitationId;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.domain.FreeGroup;
import org.springframework.hateoas.server.ExposesResourceFor;
import com.klabis.members.ActingMember;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MembersApi;
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

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@ExposesResourceFor(FreeGroup.class)
class FreeGroupController implements GroupsApi {

    private final FreeGroupManagementPort membersGroupManagementService;

    FreeGroupController(FreeGroupManagementPort membersGroupManagementService) {
        this.membersGroupManagementService = membersGroupManagementService;
    }

    @Override
    public ResponseEntity<Void> createGroup(CreateGroupRequest request, @ActingMember MemberId actingMember) {
        FreeGroup group = membersGroupManagementService.createGroup(request.name(), actingMember);

        return ResponseEntity.created(
                linkTo(methodOn(GroupsApi.class).getGroup(group.getId().uuid(), null)).toUri()
        ).build();
    }

    @Override
    public ResponseEntity<List<GroupSummaryResponse>> listGroups(@ActingMember MemberId actingMember) {

        List<FreeGroup> groups = membersGroupManagementService.listGroupsForMember(actingMember);

        HalResponseContext.setDomainList(groups);
        return ResponseEntity.ok(groups.stream()
                .map(group -> new GroupSummaryResponse(group.getId().uuid(), group.getName()))
                .toList());
    }

    // The response record is hand-written because owners/members/pendingInvitations are
    // List<EntityModel<X>> — each item carries its own _links/_templates, which the generator cannot
    // express. Everything else, the payload type included, comes from the spec.
    @Override
    public ResponseEntity<GroupResponse> getGroup(UUID id, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        FreeGroup group = membersGroupManagementService.getGroup(groupId);

        boolean isOwner = group.isOwner(actingMember);
        boolean isMember = group.hasMember(actingMember);
        if (!isOwner && !isMember) {
            throw new InsufficientAuthorityException("Free group membership or ownership required");
        }

        HalResponseContext.setDomain(group);
        return ResponseEntity.ok(toGroupResponse(group, id, isOwner));
    }

    @Override
    public ResponseEntity<Void> updateGroup(UUID id, RenameGroupRequest request, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.renameGroup(groupId, request.name(), actingMember);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteGroup(UUID id, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.deleteGroup(groupId, actingMember);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeGroupMember(UUID id, UUID memberId, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.removeMember(groupId, new MemberId(memberId), actingMember);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> addGroupOwner(UUID id, AddOwnerRequest request, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.addOwner(groupId, new MemberId(request.memberId()), actingMember);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> removeGroupOwner(UUID id, UUID memberId, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.removeOwner(groupId, new MemberId(memberId), actingMember);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> inviteMember(UUID id, InviteMemberRequest request, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        membersGroupManagementService.inviteMember(groupId, actingMember, new MemberId(request.memberId()));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> cancelInvitation(UUID id, UUID invitationId,
            CancelInvitationRequest request, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        InvitationId invId = new InvitationId(invitationId);
        String reason = request != null ? request.reason() : null;
        membersGroupManagementService.cancelInvitation(groupId, invId, actingMember, reason);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> acceptInvitation(UUID id, UUID invitationId, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        InvitationId invId = new InvitationId(invitationId);
        membersGroupManagementService.acceptInvitation(groupId, invId, actingMember);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> rejectInvitation(UUID id, UUID invitationId, @ActingMember MemberId actingMember) {

        FreeGroupId groupId = new FreeGroupId(id);
        InvitationId invId = new InvitationId(invitationId);
        membersGroupManagementService.rejectInvitation(groupId, invId, actingMember);
        return ResponseEntity.noContent().build();
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

        return new GroupResponse(group.getId().uuid(), memberModels, group.getName(), ownerModels, pendingInvitationModels);
    }

    private EntityModel<OwnerResponse> buildOwnerModel(MemberId ownerId, UUID groupUuid, boolean requestingUserIsOwner, int ownerCount) {
        EntityModel<OwnerResponse> model = EntityModel.of(new OwnerResponse(ownerId.uuid()));
        klabisLinkTo(methodOn(MembersApi.class).getMember(ownerId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        if (requestingUserIsOwner && ownerCount > 1) {
            klabisLinkTo(methodOn(GroupsApi.class).removeGroupOwner(groupUuid, ownerId.uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                    .removeGroupOwner(groupUuid, ownerId.uuid(), null)))));
        }
        return model;
    }

    private EntityModel<FreeGroupMembershipResponse> buildMemberModel(
            GroupMembership membership, UUID groupUuid, boolean isOwner, Set<MemberId> ownerIds) {

        MemberId memberId = membership.memberId();
        FreeGroupMembershipResponse response = new FreeGroupMembershipResponse(membership.joinedAt(), memberId.uuid());

        EntityModel<FreeGroupMembershipResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(MembersApi.class).getMember(memberId.uuid(), null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);

        boolean memberIsOwner = ownerIds.contains(memberId);
        if (isOwner && !memberIsOwner) {
            klabisLinkTo(methodOn(GroupsApi.class)
                    .removeGroupMember(groupUuid, memberId.uuid(), null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(GroupsApi.class)
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
        klabisLinkTo(methodOn(GroupsApi.class).getGroup(id, null)).ifPresent(link -> {
            var selfLink = link.withSelfRel();
            if (isActingMemberOwner(group)) {
                selfLink = selfLink
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class).updateGroup(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class).deleteGroup(id, null)))
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class).addGroupOwner(id, null, null)))
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class).inviteMember(id, null, null)));
            }
            dtoModel.add(selfLink);
        });

        klabisLinkTo(methodOn(GroupsApi.class).listGroups(null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
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
        klabisLinkTo(methodOn(GroupsApi.class).listGroups(null))
                .ifPresent(link -> model.add(link.withRel("groups")));
        return model;
    }
}

@MvcComponent
class GroupSummaryPostprocessor extends ModelWithDomainPostprocessor<GroupSummaryResponse, FreeGroup> {

    @Override
    public void process(EntityModel<GroupSummaryResponse> dtoModel, FreeGroup group) {
        UUID id = group.getId().uuid();
        klabisLinkTo(methodOn(GroupsApi.class).getGroup(id, null))
                .ifPresent(link -> dtoModel.add(link.withSelfRel()));
    }
}

// The self link itself is built by HalResponseBodyAdvice from the current request; this processor
// only contributes the create affordance, which stays authorization-sensitive via klabisAfford.
@MvcComponent
class GroupListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<GroupSummaryResponse>>> {

    @Override
    public CollectionModel<EntityModel<GroupSummaryResponse>> process(
            CollectionModel<EntityModel<GroupSummaryResponse>> model) {
        model.mapLink(org.springframework.hateoas.IanaLinkRelations.SELF, selfLink -> (org.springframework.hateoas.Link) selfLink
                .andAffordances(klabisAfford(methodOn(GroupsApi.class).createGroup(null, null))));
        return model;
    }
}
