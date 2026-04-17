package com.klabis.groups.freegroup.infrastructure.restapi;

import java.time.Instant;
import java.util.UUID;

record FreeGroupMembershipResponse(UUID memberId, Instant joinedAt) {
}
