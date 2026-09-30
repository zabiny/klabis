package com.klabis.members.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class AccountActivationNotAvailableException extends BusinessRuleViolationException {

    public AccountActivationNotAvailableException(MemberId memberId) {
        super("Account activation is not available for member " + memberId.uuid()
              + ": it requires a minor with an own e-mail and an account awaiting activation");
    }
}
