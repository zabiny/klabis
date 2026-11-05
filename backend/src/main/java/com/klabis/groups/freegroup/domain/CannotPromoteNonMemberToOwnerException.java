package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.members.MemberId;

import java.util.Map;

public class CannotPromoteNonMemberToOwnerException extends ConflictException implements UserFacingError {

    private final MemberId memberId;
    private final String groupName;

    public CannotPromoteNonMemberToOwnerException(MemberId memberId, String groupName) {
        super("Cannot promote member %s to owner: member must already be in the group".formatted(memberId));
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
