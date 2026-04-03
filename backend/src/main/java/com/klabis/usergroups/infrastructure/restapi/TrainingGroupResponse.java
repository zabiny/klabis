package com.klabis.usergroups.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.hateoas.EntityModel;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
record TrainingGroupResponse(UUID id, String name, Integer minAge, Integer maxAge,
                             List<EntityModel<OwnerResponse>> owners,
                             List<EntityModel<GroupMembershipResponse>> members) {
}
