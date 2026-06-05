package com.klabis.groups.common.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class CannotRemoveLastOwnerException extends BusinessRuleViolationException {

    public CannotRemoveLastOwnerException(MemberId memberId) {
        super("Member %s is the last owner of this group — designate a successor before removing".formatted(memberId));
    }

    public CannotRemoveLastOwnerException() {
        super("At least one owner must remain in the group");
    }
}
