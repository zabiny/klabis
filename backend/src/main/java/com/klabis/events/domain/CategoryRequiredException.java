package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class CategoryRequiredException extends BusinessRuleViolationException implements UserFacingError {

    public CategoryRequiredException() {
        super("Category is required for this event");
    }

    @Override
    public String field() {
        return "categoryId";
    }
}
