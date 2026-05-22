package com.klabis.common.users.domain;

import com.klabis.common.exceptions.InvalidDataException;

/**
 * Exception thrown when the current password provided by the user does not match the stored hash.
 */
public class IncorrectCurrentPasswordException extends InvalidDataException {

    public IncorrectCurrentPasswordException() {
        super("Current password is incorrect");
    }
}
