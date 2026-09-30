package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroupFilter;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.infrastructure.restapi.FamilyGroupsApi;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
public class MemberFamilyGroupLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private final FamilyGroupRepository familyGroupRepository;

    MemberFamilyGroupLinkProcessor(FamilyGroupRepository familyGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        UserId userId = new MemberId(model.getContent().id()).toUserId();
        familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(userId))
                .ifPresent(group -> klabisLinkTo(methodOn(FamilyGroupsApi.class).getFamilyGroup(group.getId().uuid(), null))
                        .map(link -> link.withRel("familyGroup"))
                        .ifPresent(model::add));
        return model;
    }
}
