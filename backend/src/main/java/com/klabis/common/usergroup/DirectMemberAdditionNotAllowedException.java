package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class DirectMemberAdditionNotAllowedException extends BusinessRuleViolationException {

    public DirectMemberAdditionNotAllowedException() {
        super("Direct member addition is not allowed for invitation-based groups. Use the invitation flow instead.");
    }
}
