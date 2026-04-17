package com.klabis.groups.membersgroup.infrastructure.restapi;

import com.klabis.common.usergroup.InvitationId;
import com.klabis.groups.membersgroup.MembersGroupId;
import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Relation(collectionRelation = "pendingInvitationResponseList")
record PendingInvitationResponse(MembersGroupId groupId, String groupName, InvitationId invitationId, UUID invitedBy) {
}
