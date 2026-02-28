package com.klabis.members.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;

/**
 * Exception thrown when the user cannot be identified from the authentication context.
 * <p>
 * This exception is thrown when the system cannot determine the user's identity
 * from the OAuth2 authentication token. This typically happens when:
 * <ul>
 *   <li>The OAuth2 token doesn't contain an email attribute</li>
 *   <li>The user's registration number cannot be found in the system</li>
 *   <li>The member exists but has no email address on file</li>
 * </ul>
 * <p>
 * This is distinct from {@link InvalidUpdateException} which indicates a validation
 * error with the update data itself.
 */
class UserIdentificationException extends BusinessRuleViolationException {

    private final String registrationNumber;

    public UserIdentificationException(String registrationNumber) {
        super(String.format(
                "Authentication failed: Unable to identify user with registration number '%s'. " +
                "Please ensure your OAuth2 token includes a valid email address or " +
                "contact support if your registration number is not recognized.",
                registrationNumber
        ));
        this.registrationNumber = registrationNumber;
    }

    public UserIdentificationException(String message, String registrationNumber) {
        super(message);
        this.registrationNumber = registrationNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }
}
