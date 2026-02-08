package com.klabis.users.passwordsetup;

/**
 * Exception thrown when token has already been used.
 */
public class TokenAlreadyUsedException extends TokenValidationException {
    public TokenAlreadyUsedException(String message) {
        super(message, Reason.USED);
    }
}
