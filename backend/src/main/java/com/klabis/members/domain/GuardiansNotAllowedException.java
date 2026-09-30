package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class GuardiansNotAllowedException extends BusinessRuleViolationException {

    private GuardiansNotAllowedException(String message) {
        super(message);
    }

    public static GuardiansNotAllowedException adultWithGuardians() {
        return new GuardiansNotAllowedException("Adult members cannot have legal guardians");
    }

    public static GuardiansNotAllowedException takeOverByMinor() {
        return new GuardiansNotAllowedException("Only an adult member can take over a legal guardian");
    }
}
