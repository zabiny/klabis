package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class InvitationNotCancellableException extends ConflictException implements UserFacingError {

    private final InvitationStatus currentStatus;

    public InvitationNotCancellableException(InvitationId invitationId, InvitationStatus currentStatus) {
        super("Cannot cancel invitation %s — current status is %s, only PENDING invitations can be cancelled"
                .formatted(invitationId, currentStatus));
        this.currentStatus = currentStatus;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("invitationStatus", currentStatus.name());
    }
}
