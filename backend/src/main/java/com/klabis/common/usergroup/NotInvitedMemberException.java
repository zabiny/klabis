package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class NotInvitedMemberException extends BusinessRuleViolationException {

    public NotInvitedMemberException(UserId userId, InvitationId invitationId) {
        super("User %s is not the invited user for invitation %s".formatted(userId, invitationId));
    }
}
