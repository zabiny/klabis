package com.klabis.usergroups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;

public class NotGroupOwnerException extends BusinessRuleViolationException {

    public NotGroupOwnerException(MemberId memberId, UserGroupId groupId) {
        super("Member %s is not an owner of group %s".formatted(memberId, groupId));
    }
}
