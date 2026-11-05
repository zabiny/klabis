package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;

public class MemberRegistrationBlockedException extends BusinessRuleViolationException implements UserFacingError {

    public MemberRegistrationBlockedException(MemberId memberId) {
        super("Member " + memberId + " is blocked from event registrations due to a missed fee selection");
    }
}
