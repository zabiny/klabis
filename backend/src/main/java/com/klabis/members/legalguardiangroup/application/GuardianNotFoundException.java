package com.klabis.members.legalguardiangroup.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class GuardianNotFoundException extends BusinessRuleViolationException {

    public GuardianNotFoundException(UserId userId) {
        super("User %s cannot be a legal guardian".formatted(userId.uuid()));
    }
}
