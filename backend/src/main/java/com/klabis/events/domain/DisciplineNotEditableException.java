package com.klabis.events.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

/**
 * Thrown when a manager tries to edit a discipline that is paired to ORIS, active or
 * retired: ORIS is that discipline's
 * sole source of truth for {@code code}/{@code name} once paired, so a manual edit is
 * refused outright rather than merely discouraged.
 */
public class DisciplineNotEditableException extends ConflictException implements UserFacingError {

    private final String disciplineName;


    public DisciplineNotEditableException(String disciplineName) {
        super("Discipline '" + disciplineName + "' is paired to ORIS and cannot be edited manually");
        this.disciplineName = disciplineName;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("disciplineName", disciplineName);
    }
}
