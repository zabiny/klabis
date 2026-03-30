package com.klabis.usergroups.infrastructure.restapi;

import java.util.List;
import java.util.UUID;

record GroupResponse(UUID id, String name, List<UUID> owners, List<GroupMembershipResponse> members) {
}
