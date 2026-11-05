package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class OwnerCannotBeMemberException extends BusinessRuleViolationException implements UserFacingError {

    private final Object memberId;
    private final String groupName;

    public OwnerCannotBeMemberException(Object memberId, String groupName) {
        super("Owner %s cannot be a member of the same group".formatted(memberId));
        this.memberId = memberId;
        this.groupName = groupName;
    }

    public Object memberId() {
        return memberId;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("groupName", groupName);
    }
}
