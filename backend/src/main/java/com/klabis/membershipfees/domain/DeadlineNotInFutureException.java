package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public class DeadlineNotInFutureException extends BusinessRuleViolationException implements UserFacingError {

    private static final String START_ERROR_TYPE = "voting-deadline-not-in-future";
    private static final String CHANGE_ERROR_TYPE = "voting-deadline-in-past";

    private final LocalDate deadline;
    private final String errorType;

    private DeadlineNotInFutureException(String message, LocalDate deadline, String errorType) {
        super(message);
        this.deadline = deadline;
        this.errorType = errorType;
    }

    public static DeadlineNotInFutureException onStart(LocalDate deadline) {
        return new DeadlineNotInFutureException(
                "Voting deadline must be in the future, but was: " + deadline, deadline,
                START_ERROR_TYPE);
    }

    // Today is still a valid deadline of an active campaign, only the past is rejected
    public static DeadlineNotInFutureException onChange(LocalDate deadline) {
        return new DeadlineNotInFutureException(
                "Voting deadline must not be in the past, but was: " + deadline, deadline,
                CHANGE_ERROR_TYPE);
    }

    @Override
    public String errorType() {
        return errorType;
    }

    @Override
    public Set<String> errorTypes() {
        return Set.of(START_ERROR_TYPE, CHANGE_ERROR_TYPE);
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("deadline", deadline.toString());
    }

    @Override
    public String field() {
        return "votingDeadline";
    }
}
