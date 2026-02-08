package com.klabis.users.passwordsetup;

/**
 * Exception thrown when password validation fails.
 */
public class PasswordValidationException extends RuntimeException {
    public PasswordValidationException(String message) {
        super(message);
    }
}
