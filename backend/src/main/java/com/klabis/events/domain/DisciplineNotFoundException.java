package com.klabis.events.domain;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.events.DisciplineId;

import java.util.Map;
import java.util.Set;

public class DisciplineNotFoundException extends ResourceNotFoundException implements UserFacingError {

    private static final String BY_ID = "discipline-not-found";
    private static final String BY_ORIS_ID = "oris-discipline-not-found";

    private final String orisId;


    public DisciplineNotFoundException(DisciplineId id) {
        super("Discipline not found: " + id);
        this.orisId = null;
    }

    public DisciplineNotFoundException(String orisId) {
        super("ORIS discipline not found with ID: " + orisId);
        this.orisId = orisId;
    }

    @Override
    public String errorType() {
        return orisId == null ? BY_ID : BY_ORIS_ID;
    }

    @Override
    public Set<String> errorTypes() {
        return Set.of(BY_ID, BY_ORIS_ID);
    }

    @Override
    public Map<String, Object> messageParameters() {
        return orisId == null ? Map.of() : Map.of("orisId", orisId);
    }
}
