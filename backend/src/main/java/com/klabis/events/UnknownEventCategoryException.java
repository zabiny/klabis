package com.klabis.events;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class UnknownEventCategoryException extends BusinessRuleViolationException implements UserFacingError {

    public UnknownEventCategoryException(EventCategoryId categoryId) {
        super("Category id '" + categoryId + "' does not belong to this event");
    }
}
