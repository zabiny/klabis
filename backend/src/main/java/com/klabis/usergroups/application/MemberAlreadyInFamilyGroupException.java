package com.klabis.usergroups.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class MemberAlreadyInFamilyGroupException extends BusinessRuleViolationException {

    public MemberAlreadyInFamilyGroupException(MemberId memberId) {
        super("Member %s is already in a family group".formatted(memberId));
    }
}
