package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class DuplicateCategoryOrisIdException extends BusinessRuleViolationException implements UserFacingError {

    public DuplicateCategoryOrisIdException() {
        super("Category orisId is used more than once");
    }
}
