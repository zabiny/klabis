package com.klabis.members.membersgroup.infrastructure.restapi;

import com.klabis.members.membersgroup.domain.MembersGroupId;
import org.springframework.hateoas.EntityModel;

import java.util.List;

record GroupResponse(MembersGroupId id, String name,
                    List<EntityModel<OwnerResponse>> owners,
                    List<EntityModel<MembersGroupMembershipResponse>> members,
                    List<EntityModel<PendingInvitationResponse>> pendingInvitations) {
}
