package com.klabis.membershipfees.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

public class DuplicatePaymentRuleException extends ConflictException implements UserFacingError {

    private final EventTypeReference eventTypeId;
    private final String rankingShortName;

    public DuplicatePaymentRuleException(EventTypeReference eventTypeId, String rankingShortName) {
        super("A rule for event type " + eventTypeId + " and ranking '" + rankingShortName
                + "' already exists on this level");
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
