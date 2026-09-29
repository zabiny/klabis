package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class CannotRemoveLastOwnerException extends BusinessRuleViolationException {

    public CannotRemoveLastOwnerException(Object memberId) {
        super("Member %s is the last owner of this group — designate a successor before removing".formatted(memberId));
    }

    public CannotRemoveLastOwnerException() {
        super("At least one owner must remain in the group");
    }
}
