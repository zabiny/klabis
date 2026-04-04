package com.klabis.usergroups.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.usergroups.UserGroupId;
import org.springframework.hateoas.EntityModel;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record TrainingGroupResponse(UserGroupId id, String name, Integer minAge, Integer maxAge,
                             List<EntityModel<TrainerResponse>> trainers,
                             List<EntityModel<GroupMembershipResponse>> members) {
}
