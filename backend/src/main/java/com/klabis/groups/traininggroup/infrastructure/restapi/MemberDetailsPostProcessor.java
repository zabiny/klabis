package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component("trainingGroupMemberDetailsPostProcessor")
public class MemberDetailsPostProcessor implements RepresentationModelProcessor<EntityModel<MemberResource>> {

    private final TrainingGroupRepository trainingGroupRepository;

    MemberDetailsPostProcessor(TrainingGroupRepository trainingGroupRepository) {
        this.trainingGroupRepository = trainingGroupRepository;
    }

    @Override
    public EntityModel<MemberResource> process(EntityModel<MemberResource> model) {
        MemberId memberId = model.getContent().memberId();
        trainingGroupRepository.findOne(TrainingGroupFilter.all().withMemberIs(memberId))
                .ifPresent(group -> model.add(Link.of("/api/training-groups/" + group.getId().value(), "trainingGroup")));
        return model;
    }
}
