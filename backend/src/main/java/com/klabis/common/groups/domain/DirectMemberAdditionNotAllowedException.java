package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class DirectMemberAdditionNotAllowedException extends BusinessRuleViolationException implements UserFacingError {

    private final String groupName;

    public DirectMemberAdditionNotAllowedException(String groupName) {
        super("Direct member addition is not allowed for invitation-based groups. Use the invitation flow instead.");
        this.groupName = groupName;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("groupName", groupName);
    }
}
