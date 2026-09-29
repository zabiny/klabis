package com.klabis.members.familygroup.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;

public class MemberAlreadyInFamilyGroupException extends BusinessRuleViolationException {

    public MemberAlreadyInFamilyGroupException(UserId userId) {
        super("User " + userId + " is already part of a family group");
    }
}
