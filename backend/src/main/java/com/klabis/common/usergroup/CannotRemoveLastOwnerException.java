package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class CannotRemoveLastOwnerException extends BusinessRuleViolationException {

    public CannotRemoveLastOwnerException(UserId userId) {
        super("User %s is the last owner of this group — designate a successor before removing".formatted(userId));
    }

    public CannotRemoveLastOwnerException() {
        super("At least one owner must remain in the group");
    }
}
