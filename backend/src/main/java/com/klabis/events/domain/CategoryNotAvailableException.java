package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class CategoryNotAvailableException extends BusinessRuleViolationException implements UserFacingError {

    public CategoryNotAvailableException() {
        super("The chosen category is not available for this event");
    }

    @Override
    public String field() {
        return "categoryId";
    }
}
