package com.klabis.common.users.passwordsetup;

/**
 * Exception thrown when token has expired.
 */
public class TokenExpiredException extends TokenValidationException {
    public TokenExpiredException(String message) {
        super(message, Reason.EXPIRED);
    }
}
