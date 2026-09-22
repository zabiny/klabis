package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

/**
 * Thrown when a registration number adopted from elsewhere (e.g. imported from ORIS) is already
 * carried by another member.
 * <p>
 * The {@code UNIQUE} constraint on {@code members.registration_number} (mirrored by
 * {@code common.users.user_name}, since the registration number is also the username) is what
 * enforces this - no extra existence check is made beforehand, since that would just be a race.
 * This exception translates the resulting {@link org.springframework.dao.DataIntegrityViolationException}
 * into a domain-meaningful failure.
 */
public class RegistrationNumberAlreadyInUseException extends BusinessRuleViolationException {

    public RegistrationNumberAlreadyInUseException(RegistrationNumber registrationNumber, Throwable cause) {
        super("Registration number '" + registrationNumber.getValue() + "' is already in use", cause);
    }
}
