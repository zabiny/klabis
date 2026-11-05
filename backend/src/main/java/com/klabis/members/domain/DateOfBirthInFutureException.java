package com.klabis.members.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

public class DateOfBirthInFutureException extends InvalidDataException implements UserFacingError {

    public DateOfBirthInFutureException() {
        super("Date of birth cannot be in the future");
    }

    @Override
    public String field() {
        return "dateOfBirth";
    }
}
