package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.common.users.Authority;

import java.util.Map;

public class InvalidDelegatedAuthorityException extends BusinessRuleViolationException implements UserFacingError {

    private final String authority;

    public InvalidDelegatedAuthorityException(Authority authority) {
        super("Authority %s cannot be delegated to group owners over group members".formatted(authority.getValue()));
        this.authority = authority.getValue();
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("authority", authority);
    }
}
