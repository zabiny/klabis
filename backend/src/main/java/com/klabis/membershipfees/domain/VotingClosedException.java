package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

public class VotingClosedException extends BusinessRuleViolationException implements UserFacingError {

    public VotingClosedException() {
        super("Voting is closed — member choice is no longer allowed after the deadline");
    }
}
