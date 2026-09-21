package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.DisciplineId;

public class OrisDisciplineAlreadyMappedException extends BusinessRuleViolationException {

    public OrisDisciplineAlreadyMappedException(DisciplineId disciplineId) {
        super("Discipline " + disciplineId + " is already mapped to another event type");
    }

    /**
     * Used when a unique-constraint violation is caught at the persistence boundary
     * (TOCTOU race between the application-layer check and the save) without the
     * specific conflicting {@link DisciplineId} at hand.
     */
    public OrisDisciplineAlreadyMappedException() {
        super("A discipline in this event type is already mapped to another event type");
    }
}
