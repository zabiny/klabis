package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.usergroups.UserGroupId;
import org.springframework.hateoas.EntityModel;

import java.util.List;

record GroupResponse(UserGroupId id, String name, List<EntityModel<OwnerResponse>> owners,
                     List<EntityModel<GroupMembershipResponse>> members,
                     List<EntityModel<PendingInvitationResponse>> pendingInvitations) {
}
