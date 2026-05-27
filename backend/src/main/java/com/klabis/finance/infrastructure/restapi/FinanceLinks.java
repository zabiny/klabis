package com.klabis.finance.infrastructure.restapi;

import org.springframework.hateoas.Link;

import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

final class FinanceLinks {

    private FinanceLinks() {
    }

    static Optional<Link> accountLink(UUID memberId) {
        return klabisLinkTo(methodOn(MemberAccountController.class).getAccount(memberId, null))
                .map(link -> link.withRel("account"));
    }
}
