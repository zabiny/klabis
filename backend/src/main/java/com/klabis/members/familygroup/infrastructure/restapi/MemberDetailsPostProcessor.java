package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component("familyGroupMemberDetailsPostProcessor")
public class MemberDetailsPostProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private final FamilyGroupRepository familyGroupRepository;

    MemberDetailsPostProcessor(FamilyGroupRepository familyGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberId memberId = model.getContent().id();
        familyGroupRepository.findByMemberOrParent(memberId)
                .ifPresent(group -> model.add(Link.of("/api/family-groups/" + group.getId().uuid(), "familyGroup")));
        return model;
    }
}
