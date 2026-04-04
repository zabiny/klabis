package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class OwnerCannotBeRemovedFromGroupException extends BusinessRuleViolationException {

    public OwnerCannotBeRemovedFromGroupException(UserId userId) {
        super("Owner %s cannot be removed from the group".formatted(userId));
    }
}
