package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class VotingClosedException extends BusinessRuleViolationException {

    public VotingClosedException() {
        super("Voting is closed — member choice is no longer allowed after the deadline");
    }
}
