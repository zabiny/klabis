package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;

public class PaymentRuleNotFoundException extends ResourceNotFoundException implements UserFacingError {

    private final EventTypeReference eventTypeId;
    private final String rankingShortName;

    public PaymentRuleNotFoundException(EventTypeReference eventTypeId, String rankingShortName) {
        super("No rule found for event type " + eventTypeId + " and ranking '" + rankingShortName + "'");
        this.eventTypeId = eventTypeId;
        this.rankingShortName = rankingShortName;
    }

    public EventTypeReference eventTypeId() {
        return eventTypeId;
    }

    public String rankingShortName() {
        return rankingShortName;
    }
}
