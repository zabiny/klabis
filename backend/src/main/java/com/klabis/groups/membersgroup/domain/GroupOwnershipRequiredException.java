package com.klabis.groups.membersgroup.domain;

import com.klabis.common.exceptions.AuthorizationException;
import com.klabis.groups.membersgroup.MembersGroupId;
import com.klabis.members.MemberId;

public class GroupOwnershipRequiredException extends AuthorizationException {

    public GroupOwnershipRequiredException(MemberId memberId, MembersGroupId groupId) {
        super("Member %s is not an owner of group %s".formatted(memberId.uuid(), groupId.uuid()));
    }
}
