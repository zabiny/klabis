package com.klabis.users.authorization;

/**
 * Exception thrown when attempting to remove the MEMBERS:PERMISSIONS authority
 * from the last active user who has it.
 * <p>
 * This business rule prevents lockout by ensuring at least one user with
 * permission management capability always exists in the system.
 */
public class CannotRemoveLastPermissionManagerException extends RuntimeException {

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
