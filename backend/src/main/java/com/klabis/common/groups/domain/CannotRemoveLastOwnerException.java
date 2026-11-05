package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class CannotRemoveLastOwnerException extends BusinessRuleViolationException implements UserFacingError {

    private final Object memberId;
    private final String groupName;

    public CannotRemoveLastOwnerException(Object memberId, String groupName) {
        super("Member %s is the last owner of this group — designate a successor before removing".formatted(memberId));
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
