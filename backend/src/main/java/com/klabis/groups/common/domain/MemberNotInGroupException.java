package com.klabis.groups.common.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class MemberNotInGroupException extends BusinessRuleViolationException {

    public MemberNotInGroupException(MemberId memberId) {
        super("Member %s is not in the group".formatted(memberId));
    }
}
