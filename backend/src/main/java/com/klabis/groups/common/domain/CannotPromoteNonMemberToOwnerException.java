package com.klabis.groups.common.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class CannotPromoteNonMemberToOwnerException extends BusinessRuleViolationException {

    private final MemberId memberId;

    public CannotPromoteNonMemberToOwnerException(MemberId memberId) {
        super("Cannot promote member %s to owner: member must already be in the group".formatted(memberId));
        this.memberId = memberId;
    }

    public MemberId getMemberId() {
        return memberId;
    }
}
