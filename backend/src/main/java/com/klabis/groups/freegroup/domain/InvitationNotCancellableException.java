package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class InvitationNotCancellableException extends BusinessRuleViolationException {

    public InvitationNotCancellableException(InvitationId invitationId, InvitationStatus currentStatus) {
        super("Cannot cancel invitation %s — current status is %s, only PENDING invitations can be cancelled"
                .formatted(invitationId, currentStatus));
    }
}
