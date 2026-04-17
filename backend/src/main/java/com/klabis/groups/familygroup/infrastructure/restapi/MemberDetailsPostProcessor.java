package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.groups.common.domain.FamilyGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberResource;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

@Component("familyGroupMemberDetailsPostProcessor")
public class MemberDetailsPostProcessor implements RepresentationModelProcessor<EntityModel<MemberResource>> {

    private final FamilyGroupRepository familyGroupRepository;

    MemberDetailsPostProcessor(FamilyGroupRepository familyGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
    }

    @Override
    public EntityModel<MemberResource> process(EntityModel<MemberResource> model) {
        MemberId memberId = model.getContent().memberId();
        familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(memberId))
                .ifPresent(group -> model.add(Link.of("/api/family-groups/" + group.getId().uuid(), "familyGroup")));
        return model;
    }
}
