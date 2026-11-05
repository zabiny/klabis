package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class NegativePaymentRulePercentageException extends BusinessRuleViolationException implements UserFacingError {

    private final int percent;

    public NegativePaymentRulePercentageException(int percent) {
        super("Percentage must be non-negative, but was: " + percent);
        this.percent = percent;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("percent", percent);
    }
}
