package com.klabis.common.security.fieldsecurity;

import com.klabis.common.exceptions.AuthorizationException;

class FieldAuthorizationException extends AuthorizationException {

    FieldAuthorizationException(String fieldName, String requirement) {
        super("Access denied to field '%s'. Required: %s".formatted(fieldName, requirement));
    }
}
