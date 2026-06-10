package com.klabis.membershipfees.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.membershipfees.MembershipFeeTierId;

public class MembershipFeeTierNotFoundException extends BusinessRuleViolationException {

    public MembershipFeeTierNotFoundException(MembershipFeeTierId id) {
        super("MembershipFeeTier not found: " + id.value());
    }
}
