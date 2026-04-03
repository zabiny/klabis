package com.klabis.usergroups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class NotInvitedMemberException extends BusinessRuleViolationException {

    public NotInvitedMemberException(MemberId memberId, InvitationId invitationId) {
        super("Member %s is not the invited member for invitation %s".formatted(memberId, invitationId));
    }
}
