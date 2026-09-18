package com.klabis.sync.application;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.ExternalSystem;

/**
 * No {@link com.klabis.sync.domain.SynchronizationAdapter} is registered for the
 * given entity type and external system — enrolment is rejected (design.md tasks.md
 * 1.15).
 */
public class UnknownSyncEntityTypeException extends InvalidDataException {

    public UnknownSyncEntityTypeException(SyncEntityType entityType, ExternalSystem system) {
        super("No synchronisation adapter registered for entity type " + entityType + " and external system " + system);
    }
}
