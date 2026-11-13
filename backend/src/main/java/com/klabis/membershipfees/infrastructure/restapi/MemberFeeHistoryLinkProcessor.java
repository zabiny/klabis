package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
class MemberFeeHistoryLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        MemberDetailsResponse content = model.getContent();
        if (content == null || content.id() == null) {
            return model;
        }

        klabisLinkTo(methodOn(MemberFeeSummaryApi.class).getFeeHistory(content.id(), null))
                .ifPresent(link -> model.add(link.withRel("feeHistory")));

        return model;
    }
}
