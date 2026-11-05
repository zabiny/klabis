package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class MemberAlreadyActiveException extends BusinessRuleViolationException implements UserFacingError {

    public MemberAlreadyActiveException() {
        super("Member is already active and cannot be resumed");
    }
}
