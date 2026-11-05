package com.klabis.common.users.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

public class PasswordsDoNotMatchException extends InvalidDataException implements UserFacingError {

    public PasswordsDoNotMatchException() {
        super("Passwords do not match");
    }

    @Override
    public String field() {
        return "passwordConfirmation";
    }
}
