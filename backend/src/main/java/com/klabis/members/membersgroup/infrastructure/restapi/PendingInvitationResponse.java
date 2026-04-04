package com.klabis.members.membersgroup.infrastructure.restapi;

import com.klabis.common.usergroup.InvitationId;
import com.klabis.members.membersgroup.domain.MembersGroupId;
import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Relation(collectionRelation = "pendingInvitationResponseList")
record PendingInvitationResponse(MembersGroupId groupId, String groupName, InvitationId invitationId, UUID invitedBy) {
}
