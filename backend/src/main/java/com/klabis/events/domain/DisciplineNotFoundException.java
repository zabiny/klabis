package com.klabis.events.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.events.DisciplineId;

public class DisciplineNotFoundException extends ResourceNotFoundException {

    public DisciplineNotFoundException(DisciplineId id) {
        super("Discipline not found: " + id);
    }

    public DisciplineNotFoundException(String orisId) {
        super("ORIS discipline not found with ID: " + orisId);
    }
}
