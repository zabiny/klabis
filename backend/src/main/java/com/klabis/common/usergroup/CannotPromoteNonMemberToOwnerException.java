package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class CannotPromoteNonMemberToOwnerException extends BusinessRuleViolationException {

    private final UserId userId;

    public CannotPromoteNonMemberToOwnerException(UserId userId) {
        super("Cannot promote user %s to owner: user must already be a member of the group".formatted(userId));
        this.userId = userId;
    }

    public UserId getUserId() {
        return userId;
    }
}
