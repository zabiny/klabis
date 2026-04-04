package com.klabis.members.traininggroup.infrastructure.restapi;

import com.klabis.members.MemberId;
import com.klabis.members.TrainingGroupProvider;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component("trainingGroupMemberDetailsPostProcessor")
public class MemberDetailsPostProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private final TrainingGroupProvider trainingGroupProvider;

    MemberDetailsPostProcessor(TrainingGroupProvider trainingGroupProvider) {
        this.trainingGroupProvider = trainingGroupProvider;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberId memberId = model.getContent().id();
        trainingGroupProvider.findTrainingGroupForMember(memberId)
                .ifPresent(data -> model.add(Link.of("/api/training-groups/" + data.groupId(), "trainingGroup")));
        return model;
    }
}
