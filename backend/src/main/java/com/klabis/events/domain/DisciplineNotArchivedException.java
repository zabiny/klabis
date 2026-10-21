package com.klabis.events.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.events.DisciplineId;

/**
 * Thrown when {@code restore} is called on a discipline that is not currently archived.
 */
public class DisciplineNotArchivedException extends ConflictException {

    public DisciplineNotArchivedException(DisciplineId disciplineId) {
        super("Discipline " + disciplineId + " is not currently archived");
    }
}
