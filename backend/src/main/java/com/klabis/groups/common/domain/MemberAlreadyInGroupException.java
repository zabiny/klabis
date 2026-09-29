package com.klabis.groups.common.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class MemberAlreadyInGroupException extends BusinessRuleViolationException {

    public MemberAlreadyInGroupException(Object memberId) {
        super("Member %s is already in the group".formatted(memberId));
    }
}
