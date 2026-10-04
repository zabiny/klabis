package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class OwnerCannotBeMemberException extends BusinessRuleViolationException {

    public OwnerCannotBeMemberException(Object memberId) {
        super("Owner %s cannot be a member of the same group".formatted(memberId));
    }
}