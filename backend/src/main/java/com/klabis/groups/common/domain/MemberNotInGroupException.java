package com.klabis.groups.common.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class MemberNotInGroupException extends BusinessRuleViolationException {

    public MemberNotInGroupException(Object memberId) {
        super("Member %s is not in the group".formatted(memberId));
    }
}
