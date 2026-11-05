package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;

import java.util.Map;

public class NotInvitedMemberException extends BusinessRuleViolationException implements UserFacingError {

    private final String groupName;

    public NotInvitedMemberException(MemberId memberId, InvitationId invitationId, String groupName) {
        super("Member %s is not the invited member for invitation %s".formatted(memberId, invitationId));
        this.groupName = groupName;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("groupName", groupName);
    }
}
