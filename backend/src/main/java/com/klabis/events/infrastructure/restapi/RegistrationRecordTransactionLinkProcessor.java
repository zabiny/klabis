package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import com.klabis.finance.application.FinanceAccountLinkSupport;
import com.klabis.members.MemberId;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Adds a {@code recordTransaction} HAL link to event registration summary rows for users with FINANCE:MANAGE authority.
 * Cross-module link processor: finance module enriches events module responses.
 * Enables finance managers to open the unified transaction dialog directly from the registrations list.
 */
@MvcComponent
class RegistrationRecordTransactionLinkProcessor implements RepresentationModelProcessor<EntityModel<RegistrationSummaryDto>> {

    private final FinanceAccountLinkSupport financeAccountLinkSupport;

    RegistrationRecordTransactionLinkProcessor(FinanceAccountLinkSupport financeAccountLinkSupport) {
        this.financeAccountLinkSupport = financeAccountLinkSupport;
    }

    private static boolean callerHasFinanceManage() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> Authority.FINANCE_MANAGE.getValue().equals(a.getAuthority()));
    }

    @Override
    public EntityModel<RegistrationSummaryDto> process(EntityModel<RegistrationSummaryDto> model) {
        if (!callerHasFinanceManage()) {
            return model;
        }
        RegistrationSummaryDto dto = model.getContent();
        if (dto == null || dto.registeredMemberId() == null) {
            return model;
        }
        MemberId memberId = dto.registeredMemberId();
        UUID memberUuid = memberId.uuid();
        if (memberUuid == null) {
            return model;
        }
        financeAccountLinkSupport.accountLink(memberUuid)
                .map(link -> link.withRel("recordTransaction"))
                .ifPresent(model::add);
        return model;
    }
}
