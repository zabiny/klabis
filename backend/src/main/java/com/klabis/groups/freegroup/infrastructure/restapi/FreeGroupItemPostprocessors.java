package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.EntityModelWithDomain;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.Invitation;
import com.klabis.groups.infrastructure.restapi.FreeGroupMembershipResponse;
import com.klabis.groups.infrastructure.restapi.GroupsApi;
import com.klabis.groups.infrastructure.restapi.OwnerResponse;
import com.klabis.groups.infrastructure.restapi.PendingInvitationResponse;
import com.klabis.members.infrastructure.restapi.MembersApi;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * The group a nested item (owner, member, pending invitation) belongs to, published once per
 * {@code getGroup} request so the stateless item postprocessors can read it.
 */
record FreeGroupItemContext(FreeGroup group, boolean requestingUserIsOwner) {

    // Null outside the group-detail request: item postprocessors are global per item DTO type, so the
    // same DTO rendered elsewhere must pass through unchanged.
    static @Nullable FreeGroupItemContext current() {
        return HalResponseContext.findContext(FreeGroupItemContext.class).orElse(null);
    }
}

@MvcComponent
class OwnerItemPostprocessor implements RepresentationModelProcessor<EntityModel<OwnerResponse>> {

    @Override
    public EntityModel<OwnerResponse> process(EntityModel<OwnerResponse> model) {
        FreeGroupItemContext context = FreeGroupItemContext.current();
        if (context == null) {
            return model;
        }
        UUID groupUuid = context.group().getId().uuid();
        UUID ownerUuid = model.getContent().memberId();

        klabisLinkTo(methodOn(MembersApi.class).getMember(ownerUuid, null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        if (context.requestingUserIsOwner() && context.group().getOwners().size() > 1) {
            klabisLinkTo(methodOn(GroupsApi.class).removeGroupOwner(groupUuid, ownerUuid, null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                    .removeGroupOwner(groupUuid, ownerUuid, null)))));
        }
        return model;
    }
}

@MvcComponent
class FreeGroupMembershipItemPostprocessor implements RepresentationModelProcessor<EntityModel<FreeGroupMembershipResponse>> {

    @Override
    public EntityModel<FreeGroupMembershipResponse> process(EntityModel<FreeGroupMembershipResponse> model) {
        FreeGroupItemContext context = FreeGroupItemContext.current();
        if (context == null) {
            return model;
        }
        UUID groupUuid = context.group().getId().uuid();
        UUID memberUuid = model.getContent().memberId();

        klabisLinkTo(methodOn(MembersApi.class).getMember(memberUuid, null))
                .map(link -> link.withRel("member"))
                .ifPresent(model::add);
        if (context.requestingUserIsOwner()) {
            klabisLinkTo(methodOn(GroupsApi.class).removeGroupMember(groupUuid, memberUuid, null))
                    .ifPresent(link -> model.add(link.withSelfRel()
                            .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                    .removeGroupMember(groupUuid, memberUuid, null)))));
        }
        return model;
    }
}

// PendingInvitationResponse is also the item type of the member's own invitation list, where the
// model is an EntityModelWithDomain handled by PendingInvitationPostprocessor; only plain models
// nested in a group detail belong here.
@MvcComponent
class GroupDetailPendingInvitationPostprocessor implements RepresentationModelProcessor<EntityModel<PendingInvitationResponse>> {

    @Override
    public EntityModel<PendingInvitationResponse> process(EntityModel<PendingInvitationResponse> model) {
        if (model instanceof EntityModelWithDomain<?, ?>) {
            return model;
        }
        FreeGroupItemContext context = FreeGroupItemContext.current();
        if (context == null) {
            return model;
        }
        UUID groupUuid = context.group().getId().uuid();
        UUID invitationUuid = model.getContent().invitationId();
        Invitation invitation = context.group().getPendingInvitations().stream()
                .filter(inv -> inv.getId().value().equals(invitationUuid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Invitation " + invitationUuid + " is not pending in group " + groupUuid));

        InvitationLinks.addRecipientLinks(model, groupUuid, invitation);
        klabisLinkTo(methodOn(GroupsApi.class).cancelInvitation(groupUuid, invitationUuid, null, null))
                .ifPresent(link -> model.add(link.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                .cancelInvitation(groupUuid, invitationUuid, null, null)))));
        return model;
    }
}

final class InvitationLinks {

    private InvitationLinks() {
    }

    // TODO: "accept" and "reject" rels point to POST endpoints and should be pure affordances
    //   per backend-patterns skill. Kept as links because the frontend (GroupsPage.tsx) reads
    //   _links.accept / _links.reject directly to render action buttons.
    static void addRecipientLinks(EntityModel<PendingInvitationResponse> model, UUID groupUuid, Invitation invitation) {
        UUID invitationUuid = invitation.getId().value();
        klabisLinkTo(methodOn(MembersApi.class).getMember(invitation.getInvitedMember().uuid(), null))
                .map(link -> link.withRel("invitedMember"))
                .ifPresent(model::add);
        klabisLinkTo(methodOn(GroupsApi.class).acceptInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("accept")
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                .acceptInvitation(groupUuid, invitationUuid, null)))));
        klabisLinkTo(methodOn(GroupsApi.class).rejectInvitation(groupUuid, invitationUuid, null))
                .ifPresent(link -> model.add(link.withRel("reject")
                        .andAffordances(klabisAfford(methodOn(GroupsApi.class)
                                .rejectInvitation(groupUuid, invitationUuid, null)))));
    }
}
