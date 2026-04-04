package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class DuplicatePendingInvitationException extends BusinessRuleViolationException {

    public DuplicatePendingInvitationException(UserId userId) {
        super("User %s already has a pending invitation to this group".formatted(userId));
    }
}
