package com.klabis.events.domain;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;

import java.util.Map;

/**
 * Thrown when {@code restore} is called on a discipline that is not currently archived.
 */
public class DisciplineNotArchivedException extends ConflictException implements UserFacingError {

    private final String disciplineName;


    public DisciplineNotArchivedException(String disciplineName) {
        super("Discipline '" + disciplineName + "' is not currently archived");
        this.disciplineName = disciplineName;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("disciplineName", disciplineName);
    }
}
