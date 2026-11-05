package com.klabis.common.exceptions;

public class InsufficientAuthorityException extends AuthorizationException implements UserFacingError {

    public InsufficientAuthorityException(String requiredAuthority) {
        super("Required authority: " + requiredAuthority);
    }
}
