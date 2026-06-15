package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.membershipfees.MembershipFeeGroupId;

public class MembershipFeeGroupNotFoundException extends BusinessRuleViolationException {

    public MembershipFeeGroupNotFoundException(MembershipFeeGroupId id) {
        super("MembershipFeeGroup not found: " + id.value());
    }
}
