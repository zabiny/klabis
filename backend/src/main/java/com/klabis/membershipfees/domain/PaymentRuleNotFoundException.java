package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class PaymentRuleNotFoundException extends BusinessRuleViolationException {

    public PaymentRuleNotFoundException(EventTypeReference eventTypeId, String rankingShortName) {
        super("No rule found for event type " + eventTypeId + " and ranking '" + rankingShortName + "'");
    }
}
