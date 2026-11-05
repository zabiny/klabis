package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;

import java.util.Map;

public class DuplicatePendingInvitationException extends BusinessRuleViolationException implements UserFacingError {

    private final MemberId memberId;
    private final String groupName;

    public DuplicatePendingInvitationException(MemberId memberId, String groupName) {
        super("Member %s already has a pending invitation to this group".formatted(memberId));
        this.memberId = memberId;
        this.groupName = groupName;
    }

    public MemberId getMemberId() {
        return memberId;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("groupName", groupName);
    }

    @Override
    public String field() {
        return "memberId";
    }
}
