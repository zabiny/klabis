package com.klabis.common.exceptions;

public class InsufficientAuthorityException extends AuthorizationException {

    public InsufficientAuthorityException(String requiredAuthority) {
        super("Required authority: " + requiredAuthority);
    }
}
