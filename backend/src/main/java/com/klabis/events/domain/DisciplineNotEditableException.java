package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.DisciplineId;

/**
 * Thrown when a manager tries to edit a discipline that is paired to ORIS, active or
 * retired (design.md D7 of {@code sync-oris-disciplines}): ORIS is that discipline's
 * sole source of truth for {@code code}/{@code name} once paired, so a manual edit is
 * refused outright rather than merely discouraged.
 */
public class DisciplineNotEditableException extends BusinessRuleViolationException {

    public DisciplineNotEditableException(DisciplineId disciplineId) {
        super("Discipline " + disciplineId + " is paired to ORIS and cannot be edited manually");
    }
}
