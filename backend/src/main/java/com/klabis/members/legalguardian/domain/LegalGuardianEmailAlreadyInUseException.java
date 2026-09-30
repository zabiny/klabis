package com.klabis.members.legalguardian.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class LegalGuardianEmailAlreadyInUseException extends BusinessRuleViolationException {

    public LegalGuardianEmailAlreadyInUseException(String email) {
        super("E-mail %s already belongs to an existing legal guardian or adult member; choose the existing person instead"
                .formatted(email));
    }
}
