package com.klabis.members.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;

public class NotSoleGuardianGroupException extends BusinessRuleViolationException implements UserFacingError {

    public NotSoleGuardianGroupException(LegalGuardianGroupId groupId) {
        super("Legal guardian group %s does not exist or the member is not its sole guardian"
                .formatted(groupId.uuid()));
    }
}
