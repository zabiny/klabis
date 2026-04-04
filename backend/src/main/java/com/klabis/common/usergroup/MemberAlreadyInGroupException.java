package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class MemberAlreadyInGroupException extends BusinessRuleViolationException {

    public MemberAlreadyInGroupException(UserId userId) {
        super("User %s is already in the group".formatted(userId));
    }
}
