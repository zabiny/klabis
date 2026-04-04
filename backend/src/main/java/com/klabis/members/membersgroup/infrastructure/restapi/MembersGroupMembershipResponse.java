package com.klabis.members.membersgroup.infrastructure.restapi;

import java.time.Instant;
import java.util.UUID;

record MembersGroupMembershipResponse(UUID memberId, Instant joinedAt) {
}
