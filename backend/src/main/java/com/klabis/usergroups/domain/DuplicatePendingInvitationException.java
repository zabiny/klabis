package com.klabis.usergroups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class DuplicatePendingInvitationException extends BusinessRuleViolationException {

    public DuplicatePendingInvitationException(MemberId memberId) {
        super("Member %s already has a pending invitation to this group".formatted(memberId));
    }
}
