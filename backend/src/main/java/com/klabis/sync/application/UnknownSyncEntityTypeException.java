package com.klabis.sync.application;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.ExternalSystem;

import java.util.Map;

/**
 * No {@link com.klabis.sync.domain.SynchronizationAdapter} is registered for the
 * given entity type and external system — enrolment is rejected (design.md tasks.md
 * 1.15).
 */
public class UnknownSyncEntityTypeException extends InvalidDataException implements UserFacingError {

    private final SyncEntityType entityType;
    private final ExternalSystem system;

    public UnknownSyncEntityTypeException(SyncEntityType entityType, ExternalSystem system) {
        super("No synchronisation adapter registered for entity type " + entityType + " and external system " + system);
        this.entityType = entityType;
        this.system = system;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("entityType", entityType.name(), "externalSystem", system.name());
    }
}
