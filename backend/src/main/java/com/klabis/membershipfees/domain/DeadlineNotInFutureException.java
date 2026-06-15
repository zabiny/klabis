package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

import java.time.LocalDate;

public class DeadlineNotInFutureException extends BusinessRuleViolationException {

    public DeadlineNotInFutureException(LocalDate deadline) {
        super("Voting deadline must be in the future, but was: " + deadline);
    }
}
