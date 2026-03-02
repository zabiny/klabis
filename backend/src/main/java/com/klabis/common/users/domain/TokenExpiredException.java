package com.klabis.common.users.domain;

/**
 * Exception thrown when token has expired.
 */
public class TokenExpiredException extends TokenValidationException {
    public TokenExpiredException(String message) {
        super(message, Reason.EXPIRED);
    }
}
