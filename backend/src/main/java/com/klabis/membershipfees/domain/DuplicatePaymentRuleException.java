package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class DuplicatePaymentRuleException extends BusinessRuleViolationException {

    public DuplicatePaymentRuleException(EventTypeReference eventTypeId, String rankingShortName) {
        super("A rule for event type " + eventTypeId + " and ranking '" + rankingShortName
                + "' already exists on this level");
    }
}
