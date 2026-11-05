package com.klabis.members.legalguardiangroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class LegalGuardianGroupWithoutGuardianException extends BusinessRuleViolationException implements UserFacingError {

    public LegalGuardianGroupWithoutGuardianException() {
        super("A minor must have at least one legal guardian");
    }
}
