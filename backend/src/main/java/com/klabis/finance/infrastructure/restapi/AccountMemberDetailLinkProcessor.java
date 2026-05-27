package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.members.MemberId;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static com.klabis.finance.infrastructure.restapi.FinanceSecurityHelper.callerHasFinanceManage;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Adds an {@code account} HAL link to member detail responses for users with FINANCE:MANAGE authority.
 * Cross-module link processor: finance module enriches members module responses.
 * Uses RepresentationModelProcessor directly (not ModelWithDomainPostprocessor) to avoid
 * a dependency on the non-exposed Member domain type.
 */
@MvcComponent
class AccountMemberDetailLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        if (!callerHasFinanceManage()) {
            return model;
        }
        MemberDetailsResponse response = model.getContent();
        if (response == null) {
            return model;
        }
        MemberId memberId = response.id();
        if (memberId == null) {
            return model;
        }
        UUID memberUuid = memberId.uuid();
        if (memberUuid == null) {
            return model;
        }
        klabisLinkTo(methodOn(MemberAccountController.class).getAccount(memberUuid, null))
                .ifPresent(link -> model.add(link.withRel("account")));
        return model;
    }

}
