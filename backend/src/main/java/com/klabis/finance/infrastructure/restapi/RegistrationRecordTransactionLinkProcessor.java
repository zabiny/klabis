package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.events.infrastructure.restapi.RegistrationSummaryDto;
import com.klabis.members.MemberId;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

import java.util.UUID;

import static com.klabis.finance.infrastructure.restapi.FinanceSecurityHelper.callerHasFinanceManage;

/**
 * Adds a {@code recordTransaction} HAL link to event registration summary rows for users with FINANCE:MANAGE authority.
 * Cross-module link processor: finance module enriches events module responses.
 * Enables finance managers to open the unified transaction dialog directly from the registrations list.
 */
@MvcComponent
class RegistrationRecordTransactionLinkProcessor implements RepresentationModelProcessor<EntityModel<RegistrationSummaryDto>> {

    @Override
    public EntityModel<RegistrationSummaryDto> process(EntityModel<RegistrationSummaryDto> model) {
        if (!callerHasFinanceManage()) {
            return model;
        }
        RegistrationSummaryDto dto = model.getContent();
        if (dto == null || dto.registeredMemberId() == null) {
            return model;
        }
        UUID memberUuid = dto.registeredMemberId().uuid();
        if (memberUuid == null) {
            return model;
        }
        FinanceLinks.accountLink(memberUuid)
                .map(link -> link.withRel("recordTransaction"))
                .ifPresent(model::add);
        return model;
    }
}
