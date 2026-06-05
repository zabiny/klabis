package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class CannotInviteExistingMemberException extends BusinessRuleViolationException {

    public CannotInviteExistingMemberException(MemberId memberId) {
        super("Member %s is already a member or owner of this group".formatted(memberId));
    }
}
