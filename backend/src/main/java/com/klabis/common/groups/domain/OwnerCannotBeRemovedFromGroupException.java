package com.klabis.common.groups.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class OwnerCannotBeRemovedFromGroupException extends BusinessRuleViolationException {

    public OwnerCannotBeRemovedFromGroupException(Object memberId) {
        super("Owner %s cannot be removed from the group".formatted(memberId));
    }
}
