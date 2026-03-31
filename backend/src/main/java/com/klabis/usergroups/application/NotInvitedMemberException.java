package com.klabis.usergroups.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;
import com.klabis.usergroups.domain.InvitationId;

public class NotInvitedMemberException extends BusinessRuleViolationException {

    public NotInvitedMemberException(MemberId memberId, InvitationId invitationId) {
        super("Member %s is not the invited member for invitation %s".formatted(memberId, invitationId));
    }
}
