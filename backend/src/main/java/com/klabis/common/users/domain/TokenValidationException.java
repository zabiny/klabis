package com.klabis.common.users.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

/**
 * Exception thrown when token validation fails.
 */
public class TokenValidationException extends InvalidDataException implements UserFacingError {
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

    @Override
    public String errorType() {
        return "invalid-token";
    }

    public enum Reason {
        INVALID,
        EXPIRED,
        USED,
        USER_NOT_FOUND,
        ACCOUNT_STATUS
    }
}
