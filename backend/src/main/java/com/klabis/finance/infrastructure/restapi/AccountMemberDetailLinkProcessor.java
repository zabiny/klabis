package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.UUID;


/**
 * Adds an {@code account} HAL link to member detail responses for users with FINANCE:MANAGE authority.
 * Cross-module link processor: finance module enriches members module responses.
 * Uses RepresentationModelProcessor directly (not ModelWithDomainPostprocessor) to avoid
 * a dependency on the non-exposed Member domain type.
 */
@MvcComponent
class AccountMemberDetailLinkProcessor implements RepresentationModelProcessor<EntityModel<MemberDetailsResponse>> {

    private final AuthorizationEvaluator authorizationEvaluator;

    AccountMemberDetailLinkProcessor(AuthorizationEvaluator authorizationEvaluator) {
        this.authorizationEvaluator = authorizationEvaluator;
    }

    @Override
    public EntityModel<MemberDetailsResponse> process(EntityModel<MemberDetailsResponse> model) {
        // getAccount is also open to the account owner (x-klabis-owner-visible); the link is for
        // managers browsing someone else's profile, so it stays hidden on the owner's own profile.
        if (!authorizationEvaluator.has(Authority.FINANCE_MANAGE)) {
            return model;
        }
        MemberDetailsResponse response = model.getContent();
        if (response == null) {
            return model;
        }
        UUID memberUuid = response.id();
        if (memberUuid == null) {
            return model;
        }
        FinanceLinks.accountLink(memberUuid).ifPresent(model::add);
        return model;
    }

}
