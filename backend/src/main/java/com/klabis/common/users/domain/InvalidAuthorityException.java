package com.klabis.common.users.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class InvalidAuthorityException extends InvalidDataException implements UserFacingError {

    private final String authority;

    public InvalidAuthorityException(String authority, String validAuthorities) {
        super("Invalid authority: " + authority + ". Valid authorities: " + validAuthorities);
        this.authority = authority;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("authority", authority);
    }
}
