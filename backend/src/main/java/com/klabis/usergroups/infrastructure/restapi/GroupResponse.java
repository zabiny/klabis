package com.klabis.usergroups.infrastructure.restapi;

import org.springframework.hateoas.EntityModel;

import java.util.List;
import java.util.UUID;

record GroupResponse(UUID id, String name, List<OwnerResponse> owners, List<EntityModel<GroupMembershipResponse>> members) {
}
