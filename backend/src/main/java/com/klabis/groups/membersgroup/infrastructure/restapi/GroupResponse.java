package com.klabis.groups.membersgroup.infrastructure.restapi;

import com.klabis.groups.membersgroup.MembersGroupId;
import org.springframework.hateoas.EntityModel;

import java.util.List;

record GroupResponse(MembersGroupId id, String name,
                    List<EntityModel<OwnerResponse>> owners,
                    List<EntityModel<MembersGroupMembershipResponse>> members,
                    List<EntityModel<PendingInvitationResponse>> pendingInvitations) {
}
