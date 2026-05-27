package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.ui.RootModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

@MvcComponent
class AccountRootLinkProcessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof KlabisJwtAuthenticationToken token) || !token.hasMemberProfile()) {
            return model;
        }
        UUID memberId = token.getMemberIdUuid().orElse(null);
        if (memberId == null) {
            return model;
        }
        FinanceLinks.accountLink(memberId).ifPresent(model::add);
        return model;
    }
}
