package com.klabis.common.exceptions;

public class MemberProfileRequiredException extends AuthorizationException {

    public MemberProfileRequiredException() {
        super("User must have a member profile to perform this action");
    }
}
