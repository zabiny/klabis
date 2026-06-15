package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class MemberRegistrationBlockedException extends BusinessRuleViolationException {

    public MemberRegistrationBlockedException(MemberId memberId) {
        super("Member " + memberId + " is blocked from event registrations due to a missed fee selection");
    }
}
