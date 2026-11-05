package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class DuplicateCategoryNameException extends BusinessRuleViolationException implements UserFacingError {

    private final String categoryName;

    public DuplicateCategoryNameException(String categoryName) {
        super("Category name '" + categoryName + "' is used more than once");
        this.categoryName = categoryName;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("categoryName", categoryName);
    }
}
