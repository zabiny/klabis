package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import com.klabis.members.infrastructure.restapi.MemberSummaryResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;


/**
 * Adds an {@code account} HAL link to member summary responses (list rows) for users with FINANCE:MANAGE authority.
 * Cross-module link processor: finance module enriches members module responses.
 */
@MvcComponent
class AccountMemberSummaryLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberSummaryResponse>> {

    private final AuthorizationEvaluator authorizationEvaluator;

    AccountMemberSummaryLinkProcessor(AuthorizationEvaluator authorizationEvaluator) {
        this.authorizationEvaluator = authorizationEvaluator;
    }

    @Override
    public EntityModel<MemberSummaryResponse> process(EntityModel<MemberSummaryResponse> model) {
        if (!authorizationEvaluator.has(Authority.FINANCE_MANAGE)) {
            return model;
        }
        MemberSummaryResponse response = model.getContent();
        if (response == null || response.id() == null) {
            return model;
        }
        FinanceLinks.accountLink(response.id()).ifPresent(model::add);
        return model;
    }

}
