package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class RegistrationDeadlineAfterEventDateException extends BusinessRuleViolationException implements UserFacingError {

    public RegistrationDeadlineAfterEventDateException() {
        super("Registration deadline must be on or before event date");
    }

    @Override
    public String field() {
        return "deadlines";
    }
}
