package com.klabis.members.legalguardian.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class LegalGuardianEmailAlreadyInUseException extends BusinessRuleViolationException implements UserFacingError {

    private final String email;

    public LegalGuardianEmailAlreadyInUseException(String email) {
        super("E-mail %s already belongs to an existing legal guardian or adult member; choose the existing person instead"
                .formatted(email));
        this.email = email;
    }

    @Override
    public java.util.Map<String, Object> messageParameters() {
        return java.util.Map.of("email", email);
    }
}
