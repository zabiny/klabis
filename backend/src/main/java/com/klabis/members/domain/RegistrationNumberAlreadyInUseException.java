package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

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
public class RegistrationNumberAlreadyInUseException extends BusinessRuleViolationException implements UserFacingError {

    private final String registrationNumber;

    public RegistrationNumberAlreadyInUseException(RegistrationNumber registrationNumber, Throwable cause) {
        super("Registration number '" + registrationNumber.getValue() + "' is already in use", cause);
        this.registrationNumber = registrationNumber.getValue();
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("registrationNumber", registrationNumber);
    }
}
