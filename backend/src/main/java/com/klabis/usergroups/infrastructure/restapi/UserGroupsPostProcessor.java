package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.members.FamilyGroupProvider;
import com.klabis.members.MemberId;
import com.klabis.members.TrainingGroupProvider;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component
public class UserGroupsPostProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {
    private final TrainingGroupProvider trainingGroupProvider;
    private final FamilyGroupProvider familyGroupProvider;

    UserGroupsPostProcessor(TrainingGroupProvider trainingGroupProvider, FamilyGroupProvider familyGroupProvider) {
        this.trainingGroupProvider = trainingGroupProvider;
        this.familyGroupProvider = familyGroupProvider;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberId memberId = model.getContent().id();
        trainingGroupProvider.findTrainingGroupForMember(memberId)
                .ifPresent(data -> model.add(Link.of("/api/training-groups/" + data.groupId(), "trainingGroup")));
        familyGroupProvider.findFamilyGroupForMember(memberId)
                .ifPresent(data -> model.add(Link.of("/api/family-groups/" + data.groupId(), "familyGroup")));
        return model;
    }
}
