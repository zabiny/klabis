package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.members.MemberId;

import java.util.Map;

public class GroupOwnershipRequiredException extends AuthorizationException implements UserFacingError {

    private final String groupName;

    public GroupOwnershipRequiredException(MemberId memberId, FreeGroupId groupId, String groupName) {
        super("Member %s is not an owner of group %s".formatted(memberId.uuid(), groupId.uuid()));
        this.groupName = groupName;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("groupName", groupName);
    }
}
