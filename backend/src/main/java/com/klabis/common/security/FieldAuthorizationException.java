package com.klabis.common.security;

import com.klabis.common.exceptions.AuthorizationException;

class FieldAuthorizationException extends AuthorizationException {

    FieldAuthorizationException(String fieldName, String requiredAuthority) {
        super("Access denied to field '%s'. Required authority: %s".formatted(fieldName, requiredAuthority));
    }
}
