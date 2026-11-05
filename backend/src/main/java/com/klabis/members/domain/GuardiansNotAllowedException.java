package com.klabis.members.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Set;

public class GuardiansNotAllowedException extends BusinessRuleViolationException implements UserFacingError {

    private static final String ADULT_WITH_GUARDIANS = "adult-member-cannot-have-guardians";
    private static final String TAKE_OVER_BY_MINOR = "minor-cannot-take-over-guardian";

    private final String errorType;
    private final String field;

    private GuardiansNotAllowedException(String message, String errorType, String field) {
        super(message);
        this.errorType = errorType;
        this.field = field;
    }

    public static GuardiansNotAllowedException adultWithGuardians() {
        return new GuardiansNotAllowedException("Adult members cannot have legal guardians",
                ADULT_WITH_GUARDIANS, "legalGuardians");
    }

    public static GuardiansNotAllowedException takeOverByMinor() {
        return new GuardiansNotAllowedException("Only an adult member can take over a legal guardian",
                TAKE_OVER_BY_MINOR, "legalGuardianUserId");
    }

    @Override
    public String errorType() {
        return errorType;
    }

    @Override
    public Set<String> errorTypes() {
        return Set.of(ADULT_WITH_GUARDIANS, TAKE_OVER_BY_MINOR);
    }

    @Override
    public String field() {
        return field;
    }
}
