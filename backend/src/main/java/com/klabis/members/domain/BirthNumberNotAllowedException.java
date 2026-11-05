package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class BirthNumberNotAllowedException extends BusinessRuleViolationException implements UserFacingError {

    public BirthNumberNotAllowedException() {
        super("Birth number is only allowed for Czech nationals");
    }

    @Override
    public String field() {
        return "birthNumber";
    }
}
