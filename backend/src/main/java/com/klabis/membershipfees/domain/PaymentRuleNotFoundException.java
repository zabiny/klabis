package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;

public class PaymentRuleNotFoundException extends ResourceNotFoundException {

    public PaymentRuleNotFoundException(EventTypeReference eventTypeId, String rankingShortName) {
        super("No rule found for event type " + eventTypeId + " and ranking '" + rankingShortName + "'");
    }
}
