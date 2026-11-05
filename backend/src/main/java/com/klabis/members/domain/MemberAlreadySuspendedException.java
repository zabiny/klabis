package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class MemberAlreadySuspendedException extends BusinessRuleViolationException implements UserFacingError {

    public MemberAlreadySuspendedException() {
        super("Member is already suspended and cannot be suspended again");
    }
}
