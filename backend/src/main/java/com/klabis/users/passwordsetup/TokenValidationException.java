package com.klabis.users.passwordsetup;

/**
 * Exception thrown when token validation fails.
 */
public class TokenValidationException extends RuntimeException {
    private final Reason reason;

    public TokenValidationException(String message) {
        this(message, Reason.INVALID);
    }

    public TokenValidationException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        INVALID,
        EXPIRED,
        USED,
        USER_NOT_FOUND,
        ACCOUNT_STATUS
    }
}
