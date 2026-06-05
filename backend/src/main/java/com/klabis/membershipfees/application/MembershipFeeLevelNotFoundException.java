package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.membershipfees.MembershipFeeLevelId;

public class MembershipFeeLevelNotFoundException extends BusinessRuleViolationException {

    public MembershipFeeLevelNotFoundException(MembershipFeeLevelId id) {
        super("MembershipFeeLevel not found: " + id.value());
    }
}
