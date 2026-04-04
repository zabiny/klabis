package com.klabis.members.familygroup.infrastructure.restapi;

import java.time.Instant;
import java.util.UUID;

record FamilyGroupMembershipResponse(UUID memberId, Instant joinedAt) {
}
