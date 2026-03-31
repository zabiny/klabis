package com.klabis.usergroups.infrastructure.restapi;

import java.time.Instant;
import java.util.UUID;

record GroupMembershipResponse(UUID memberId, String firstName, String lastName, String registrationNumber, Instant joinedAt) {
}
