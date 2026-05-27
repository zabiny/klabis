package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.members.infrastructure.restapi.MemberSummaryResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.UUID;

import static com.klabis.finance.infrastructure.restapi.FinanceSecurityHelper.callerHasFinanceManage;

/**
 * Adds an {@code account} HAL link to member summary responses (list rows) for users with FINANCE:MANAGE authority.
 * Cross-module link processor: finance module enriches members module responses.
 */
@MvcComponent
class AccountMemberSummaryLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberSummaryResponse>> {

    @Override
    public EntityModel<MemberSummaryResponse> process(EntityModel<MemberSummaryResponse> model) {
        if (!callerHasFinanceManage()) {
            return model;
        }
        MemberSummaryResponse response = model.getContent();
        if (response == null || response.id() == null) {
            return model;
        }
        UUID memberUuid = response.id().uuid();
        if (memberUuid == null) {
            return model;
        }
        FinanceLinks.accountLink(memberUuid).ifPresent(model::add);
        return model;
    }

}
