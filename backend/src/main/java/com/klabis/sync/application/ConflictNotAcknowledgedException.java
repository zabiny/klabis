package com.klabis.sync.application;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.sync.SyncRecordId;

/**
 * A resolution was attempted before the conflict was acknowledged, or the
 * acknowledgement no longer matches the record's current hash pair — a side moved
 * since the manager acknowledged and the record's snapshots were refreshed
 * (design.md D7).
 */
public class ConflictNotAcknowledgedException extends ConflictException implements UserFacingError {

    public ConflictNotAcknowledgedException(SyncRecordId id) {
        super("Synchronisation record " + id + " has no current acknowledgement for its present conflict");
    }
}
