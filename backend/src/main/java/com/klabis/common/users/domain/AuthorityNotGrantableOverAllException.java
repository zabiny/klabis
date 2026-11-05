package com.klabis.common.users.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.users.Authority;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Thrown when an authority that may be held only over specific targets is assigned to a user directly, i.e. over
 * everything. Such authorities come from relationships only.
 */
public class AuthorityNotGrantableOverAllException extends BusinessRuleViolationException implements UserFacingError {

    private final List<String> authorities;

    public AuthorityNotGrantableOverAllException(Set<Authority> authorities) {
        super("Authorities cannot be granted over everything, only through relationships: "
                + String.join(", ", codes(authorities)));
        this.authorities = codes(authorities);
    }

    private static List<String> codes(Set<Authority> authorities) {
        return authorities.stream().map(Authority::getValue).sorted().toList();
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("authorities", authorities);
    }
}
