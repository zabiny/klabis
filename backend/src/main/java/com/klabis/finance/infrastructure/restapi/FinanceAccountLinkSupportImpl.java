package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.finance.application.FinanceAccountLinkSupport;
import org.springframework.hateoas.Link;

import java.util.Optional;
import java.util.UUID;

@MvcComponent
class FinanceAccountLinkSupportImpl implements FinanceAccountLinkSupport {

    @Override
    public Optional<Link> accountLink(UUID memberUuid) {
        return FinanceLinks.accountLink(memberUuid);
    }
}
