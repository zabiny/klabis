package com.klabis.usergroups.infrastructure.restapi;

import java.time.Instant;
import java.util.UUID;

record GroupMembershipResponse(UUID memberId, Instant joinedAt) {
}
