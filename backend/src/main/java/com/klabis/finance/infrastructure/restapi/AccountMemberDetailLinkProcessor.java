package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.UUID;


/**
 * Adds an {@code account} HAL link to member detail responses for users with FINANCE:MANAGE authority
 * and for users holding FINANCE:VIEW over the displayed member (legal guardians), except on their own profile.
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
        MemberDetailsResponse response = model.getContent();
        if (response == null || response.id() == null) {
            return model;
        }
        UUID memberUuid = response.id();
        if (mayOpenAccountFromDetail(TargetRef.member(memberUuid))) {
            FinanceLinks.accountLink(memberUuid).ifPresent(model::add);
        }
        return model;
    }

    // The account owner holds FINANCE:VIEW over themself, but reaches their own account from the main menu,
    // so the link stays on profiles of other members only.
    private boolean mayOpenAccountFromDetail(TargetRef member) {
        return authorizationEvaluator.has(Authority.FINANCE_MANAGE)
                || (authorizationEvaluator.has(Authority.FINANCE_VIEW, member) && !authorizationEvaluator.isSelf(member));
    }

}
