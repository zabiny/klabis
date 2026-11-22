package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.RootModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;

@MvcComponent
class AccountRootLinkProcessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    private final ViewableAccounts viewableAccounts;

    AccountRootLinkProcessor(ViewableAccounts viewableAccounts) {
        this.viewableAccounts = viewableAccounts;
    }

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        viewableAccounts.inOrder().stream()
                .findFirst()
                .flatMap(account -> FinanceLinks.accountLink(account.memberId()))
                .ifPresent(model::add);
        return model;
    }
}
