package com.klabis.common.users.passwordsetup;

import com.klabis.common.exceptions.InvalidDataException;

/**
 * Exception thrown when password validation fails.
 */
public class PasswordValidationException extends InvalidDataException {
    public PasswordValidationException(String message) {
        super(message);
    }
}
