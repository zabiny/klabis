package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;

public class InvitationNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public InvitationNotFoundException(InvitationId invitationId) {
        super("Pending invitation not found: %s".formatted(invitationId));
    }
}
