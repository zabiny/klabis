package com.klabis.members.management;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.members.RegistrationNumber;

/**
 * Exception thrown when a member attempts to edit another member's information.
 * <p>
 * This exception is thrown when the authenticated user's email does not match
 * the email of the member they are attempting to update.
 */
class SelfEditNotAllowedException extends AuthorizationException {

    private final RegistrationNumber authenticatedUser;
    private final RegistrationNumber targetMember;

    public SelfEditNotAllowedException(RegistrationNumber authenticatedUser, RegistrationNumber targetMember) {
        super(String.format(
                "User with email '%s' is not authorized to edit member with email '%s'. " +
                "Members can only edit their own information.",
                authenticatedUser.getValue(), targetMember.getValue()
        ));
        this.authenticatedUser = authenticatedUser;
        this.targetMember = targetMember;
    }

    public RegistrationNumber getAuthenticatedUser() {
        return authenticatedUser;
    }

    public RegistrationNumber getTargetMember() {
        return targetMember;
    }
}
