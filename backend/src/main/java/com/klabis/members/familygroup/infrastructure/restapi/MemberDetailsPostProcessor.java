package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.members.FamilyGroupProvider;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component("familyGroupMemberDetailsPostProcessor")
public class MemberDetailsPostProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private final FamilyGroupProvider familyGroupProvider;

    MemberDetailsPostProcessor(FamilyGroupProvider familyGroupProvider) {
        this.familyGroupProvider = familyGroupProvider;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberId memberId = model.getContent().id();
        familyGroupProvider.findFamilyGroupForMember(memberId)
                .ifPresent(data -> model.add(Link.of("/api/family-groups/" + data.groupId(), "familyGroup")));
        return model;
    }
}
