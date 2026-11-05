package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class SiCardNumberRequiredException extends BusinessRuleViolationException implements UserFacingError {

    public SiCardNumberRequiredException() {
        super("SI card number is required");
    }

    @Override
    public String field() {
        return "siCardNumber";
    }
}
