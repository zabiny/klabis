package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class SnapshotFrozenException extends BusinessRuleViolationException implements UserFacingError {

    public SnapshotFrozenException() {
        super("Cannot edit snapshot of a FROZEN MembershipFeeGroup");
    }
}
