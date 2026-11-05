package com.klabis.events.infrastructure.restapi;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

class UnknownEventCategoryException extends BusinessRuleViolationException implements UserFacingError {

    UnknownEventCategoryException(Object categoryId) {
        super("Category id '" + categoryId + "' does not belong to this event");
    }
}
