package com.klabis.members.legalguardiangroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class LegalGuardianGroupWithoutGuardianException extends BusinessRuleViolationException {

    public LegalGuardianGroupWithoutGuardianException() {
        super("A minor must have at least one legal guardian");
    }
}
