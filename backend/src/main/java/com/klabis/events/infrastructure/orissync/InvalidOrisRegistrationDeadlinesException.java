package com.klabis.events.infrastructure.orissync;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

class InvalidOrisRegistrationDeadlinesException extends BusinessRuleViolationException implements UserFacingError {

    private final int orisId;

    InvalidOrisRegistrationDeadlinesException(int orisId, String reason) {
        super("ORIS event %d has invalid registration deadlines: %s".formatted(orisId, reason));
        this.orisId = orisId;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("orisId", orisId);
    }
}
