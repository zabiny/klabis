package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class MemberNotInGroupException extends BusinessRuleViolationException {

    public MemberNotInGroupException(UserId userId) {
        super("User %s is not in the group".formatted(userId));
    }
}
