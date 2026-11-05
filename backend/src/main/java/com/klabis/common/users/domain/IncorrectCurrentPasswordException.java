package com.klabis.common.users.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

public class IncorrectCurrentPasswordException extends InvalidDataException implements UserFacingError {

    public IncorrectCurrentPasswordException() {
        super("Current password is incorrect");
    }

    @Override
    public String field() {
        return "currentPassword";
    }
}
