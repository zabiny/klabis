package com.klabis.members.traininggroup.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
import org.springframework.hateoas.EntityModel;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
record TrainingGroupResponse(TrainingGroupId id, String name, Integer minAge, Integer maxAge,
                             List<EntityModel<TrainerResponse>> trainers,
                             List<EntityModel<GroupMembershipResponse>> members) {
}
