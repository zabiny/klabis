package com.klabis.events.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import com.klabis.finance.application.FinanceAccountLinkSupport;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.UUID;

/**
 * Adds a {@code recordTransaction} HAL link to event registration summary rows for users with FINANCE:MANAGE authority.
 * Cross-module link processor: finance module enriches events module responses.
 * Enables finance managers to open the unified transaction dialog directly from the registrations list.
 */
@MvcComponent
class RegistrationRecordTransactionLinkProcessor implements RepresentationModelProcessor<EntityModel<RegistrationSummaryDto>> {

    private final FinanceAccountLinkSupport financeAccountLinkSupport;
    private final AuthorizationEvaluator authorizationEvaluator;

    RegistrationRecordTransactionLinkProcessor(FinanceAccountLinkSupport financeAccountLinkSupport,
                                               AuthorizationEvaluator authorizationEvaluator) {
        this.financeAccountLinkSupport = financeAccountLinkSupport;
        this.authorizationEvaluator = authorizationEvaluator;
    }

    @Override
    public EntityModel<RegistrationSummaryDto> process(EntityModel<RegistrationSummaryDto> model) {
        // The link points at getAccount, which the account owner may also open; recording a
        // transaction is a manager action, so the owner's own registration row must not carry it.
        if (!authorizationEvaluator.has(Authority.FINANCE_MANAGE)) {
            return model;
        }
        RegistrationSummaryDto dto = model.getContent();
        if (dto == null || dto.registeredMemberId() == null) {
            return model;
        }
        UUID memberUuid = dto.registeredMemberId();
        financeAccountLinkSupport.accountLink(memberUuid)
                .map(link -> link.withRel("recordTransaction"))
                .ifPresent(model::add);
        return model;
    }
}
