package com.klabis.groups.freegroup.domain;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.members.MemberId;

public class GroupOwnershipRequiredException extends AuthorizationException {

    public GroupOwnershipRequiredException(MemberId memberId, FreeGroupId groupId) {
        super("Member %s is not an owner of group %s".formatted(memberId.uuid(), groupId.uuid()));
    }
}
