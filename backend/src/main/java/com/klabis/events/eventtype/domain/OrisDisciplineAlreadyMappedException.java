package com.klabis.events.eventtype.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;

public class OrisDisciplineAlreadyMappedException extends BusinessRuleViolationException {

    public OrisDisciplineAlreadyMappedException(int disciplineId) {
        super("ORIS discipline ID " + disciplineId + " is already mapped to another event type");
    }
}
