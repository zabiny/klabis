package com.klabis.usergroups.infrastructure.restapi;

import org.springframework.hateoas.EntityModel;

import java.util.List;
import java.util.UUID;

record TrainingGroupResponse(UUID id, String name, int minAge, int maxAge,
                             List<EntityModel<OwnerResponse>> owners,
                             List<EntityModel<GroupMembershipResponse>> members) {
}
