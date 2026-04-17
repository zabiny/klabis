package com.klabis.members.traininggroup.infrastructure.restapi;

import com.klabis.members.MemberId;
import com.klabis.members.groups.domain.TrainingGroupFilter;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component("trainingGroupMemberDetailsPostProcessor")
public class MemberDetailsPostProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private final TrainingGroupRepository trainingGroupRepository;

    MemberDetailsPostProcessor(TrainingGroupRepository trainingGroupRepository) {
        this.trainingGroupRepository = trainingGroupRepository;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberId memberId = model.getContent().id();
        trainingGroupRepository.findOne(TrainingGroupFilter.all().withMemberIs(memberId))
                .ifPresent(group -> model.add(Link.of("/api/training-groups/" + group.getId().value(), "trainingGroup")));
        return model;
    }
}
