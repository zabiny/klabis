package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class SnapshotFrozenException extends BusinessRuleViolationException {

    public SnapshotFrozenException() {
        super("Cannot edit snapshot of a FROZEN MembershipFeeGroup");
    }
}
