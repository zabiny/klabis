package com.klabis.groups.common.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class OwnerCannotBeRemovedFromGroupException extends BusinessRuleViolationException {

    public OwnerCannotBeRemovedFromGroupException(MemberId memberId) {
        super("Owner %s cannot be removed from the group".formatted(memberId));
    }
}
