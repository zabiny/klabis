package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.ConflictException;

public class DuplicatePaymentRuleException extends ConflictException {

    public DuplicatePaymentRuleException(EventTypeReference eventTypeId, String rankingShortName) {
        super("A rule for event type " + eventTypeId + " and ranking '" + rankingShortName
                + "' already exists on this level");
    }
}
