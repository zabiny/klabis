package com.klabis.common.users.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.Authority;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thrown when an authority that may be held only over specific targets is assigned to a user directly, i.e. over
 * everything. Such authorities come from relationships only.
 */
public class AuthorityNotGrantableOverAllException extends BusinessRuleViolationException {

    public AuthorityNotGrantableOverAllException(Set<Authority> authorities) {
        super("Authorities cannot be granted over everything, only through relationships: " + authorities.stream()
                .map(Authority::getValue)
                .sorted()
                .collect(Collectors.joining(", ")));
    }
}
