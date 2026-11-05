package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class PaymentRuleValueMissingException extends BusinessRuleViolationException implements UserFacingError {

    private final String ruleType;

    public PaymentRuleValueMissingException(String ruleType) {
        super("A payment rule of type " + ruleType + " requires its value");
        this.ruleType = ruleType;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("ruleType", ruleType);
    }
}
