package com.klabis.finance.application;

import org.springframework.hateoas.Link;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides HATEOAS links pointing to Finance resources.
 * Consumed by cross-module processors in other modules (e.g. events) that need to add
 * finance-related links without depending on finance.infrastructure.restapi internals.
 */
public interface FinanceAccountLinkSupport {

    Optional<Link> accountLink(UUID memberUuid);
}
