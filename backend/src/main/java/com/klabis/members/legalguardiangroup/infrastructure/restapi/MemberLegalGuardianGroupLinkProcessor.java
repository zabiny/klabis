package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.LegalGuardianGroupsApi;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
public class MemberLegalGuardianGroupLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private final LegalGuardianGroupRepository legalGuardianGroupRepository;

    MemberLegalGuardianGroupLinkProcessor(LegalGuardianGroupRepository legalGuardianGroupRepository) {
        this.legalGuardianGroupRepository = legalGuardianGroupRepository;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberId minorId = new MemberId(model.getContent().id());
        legalGuardianGroupRepository.findOne(LegalGuardianGroupFilter.all().withMinorIs(minorId.toUserId()))
                .ifPresent(group -> klabisLinkTo(methodOn(LegalGuardianGroupsApi.class).getLegalGuardianGroup(group.getId().uuid()))
                        .map(link -> link.withRel("legalGuardianGroup"))
                        .ifPresent(model::add));
        return model;
    }
}
