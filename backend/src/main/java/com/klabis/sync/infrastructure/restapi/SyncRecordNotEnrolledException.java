package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.sync.domain.SyncTarget;

/**
 * No {@link com.klabis.sync.domain.SyncRecord} is enrolled for this target — the REST
 * resource's 404 case (design.md D14 REST API section: "the entity is not enrolled").
 */
class SyncRecordNotEnrolledException extends ResourceNotFoundException {

    SyncRecordNotEnrolledException(SyncTarget target) {
        super("No synchronisation record enrolled for " + target.entityType() + " " + target.entityId());
    }
}
