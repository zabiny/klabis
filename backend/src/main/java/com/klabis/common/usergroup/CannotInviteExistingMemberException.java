package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class CannotInviteExistingMemberException extends BusinessRuleViolationException {

    public CannotInviteExistingMemberException(UserId userId) {
        super("User %s is already a member or owner of this group".formatted(userId));
    }
}
