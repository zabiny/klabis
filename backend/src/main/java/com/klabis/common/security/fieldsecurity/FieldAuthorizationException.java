package com.klabis.common.security.fieldsecurity;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.UserFacingError;

class FieldAuthorizationException extends AuthorizationException implements UserFacingError {

    private final String fieldName;

    FieldAuthorizationException(String fieldName, String requirement) {
        super("Access denied to field '%s'. Required: %s".formatted(fieldName, requirement));
        this.fieldName = fieldName;
    }

    @Override
    public String field() {
        return fieldName;
    }
}
