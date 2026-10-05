package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.Authority;

public class InvalidDelegatedAuthorityException extends BusinessRuleViolationException {

    public InvalidDelegatedAuthorityException(Authority authority) {
        super("Authority %s cannot be delegated to group owners over group members".formatted(authority.getValue()));
    }
}
