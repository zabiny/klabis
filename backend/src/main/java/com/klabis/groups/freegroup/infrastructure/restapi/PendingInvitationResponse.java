package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.usergroup.InvitationId;
import com.klabis.groups.freegroup.FreeGroupId;
import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Relation(collectionRelation = "pendingInvitationResponseList")
record PendingInvitationResponse(FreeGroupId groupId, String groupName, InvitationId invitationId, UUID invitedBy) {
}
