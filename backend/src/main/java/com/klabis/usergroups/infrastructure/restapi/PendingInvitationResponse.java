package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.domain.InvitationId;
import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Relation(collectionRelation = "pendingInvitationResponseList")
record PendingInvitationResponse(UserGroupId groupId, String groupName, InvitationId invitationId, UUID invitedBy) {
}
