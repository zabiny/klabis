package com.klabis.groups.common.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class MemberAlreadyInGroupException extends BusinessRuleViolationException {

    public MemberAlreadyInGroupException(MemberId memberId) {
        super("Member %s is already in the group".formatted(memberId));
    }
}
