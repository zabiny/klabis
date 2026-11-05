package com.klabis.common.users.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

public class CannotRemoveLastPermissionManagerException extends ConflictException implements UserFacingError {

    private static final String MESSAGE =
            "Cannot remove MEMBERS:PERMISSIONS: this is the last active user with permission management capability. " +
            "Please grant MEMBERS:PERMISSIONS to another user before removing it from this user.";

    public CannotRemoveLastPermissionManagerException() {
        super(MESSAGE);
    }

    public CannotRemoveLastPermissionManagerException(String message) {
        super(message);
    }
}
