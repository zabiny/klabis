package com.klabis.usergroups.infrastructure.restapi;

import org.springframework.hateoas.server.core.Relation;

import java.util.UUID;

@Relation(collectionRelation = "pendingInvitationResponseList")
record PendingInvitationResponse(UUID groupId, String groupName, UUID invitationId, UUID invitedBy) {
}
