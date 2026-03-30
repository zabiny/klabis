package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.exceptions.AuthorizationException;

class MemberProfileRequiredException extends AuthorizationException {

    MemberProfileRequiredException() {
        super("A member profile is required to manage groups");
    }
}
