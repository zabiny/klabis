package com.klabis.groups.familygroup.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class MemberAlreadyInFamilyGroupException extends BusinessRuleViolationException {

    public MemberAlreadyInFamilyGroupException(MemberId memberId) {
        super("Member " + memberId + " is already part of a family group");
    }
}
