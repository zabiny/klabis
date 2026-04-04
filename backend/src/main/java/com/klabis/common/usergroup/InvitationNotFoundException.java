package com.klabis.common.usergroup;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class InvitationNotFoundException extends BusinessRuleViolationException {

    public InvitationNotFoundException(InvitationId invitationId) {
        super("Pending invitation not found: %s".formatted(invitationId));
    }
}
