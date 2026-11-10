package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

public class InvalidEventStatusTransitionException extends BusinessRuleViolationException implements UserFacingError {

    private final EventStatus from;
    private final EventStatus to;

    public InvalidEventStatusTransitionException(EventStatus from, EventStatus to) {
        super("Cannot transition from %s to %s".formatted(from, to));
        this.from = from;
        this.to = to;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("from", from.name(), "to", to.name());
    }
}
