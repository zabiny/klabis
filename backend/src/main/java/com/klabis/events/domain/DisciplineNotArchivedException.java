package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.DisciplineId;

/**
 * Thrown when {@code restore} is called on a discipline that is not currently archived
 * (design.md D8 of {@code sync-oris-disciplines}: "409 Conflict if not currently
 * archived"). Mirrors the sync module's own "wrong state" exceptions (e.g. {@code
 * SyncRecordNotFailedException}), which this codebase maps to {@code 409} via a local
 * {@code @RestControllerAdvice} rather than the shared handler's default {@code 400} —
 * the REST-layer mapping for this exception is added in task 9.2.
 */
public class DisciplineNotArchivedException extends BusinessRuleViolationException {

    public DisciplineNotArchivedException(DisciplineId disciplineId) {
        super("Discipline " + disciplineId + " is not currently archived");
    }
}
