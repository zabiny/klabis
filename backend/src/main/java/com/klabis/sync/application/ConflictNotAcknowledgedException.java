package com.klabis.sync.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.sync.SyncRecordId;

/**
 * A resolution was attempted before the conflict was acknowledged, or the
 * acknowledgement no longer matches the record's current hash pair — a side moved
 * since the manager acknowledged and the record's snapshots were refreshed
 * (design.md D7).
 */
public class ConflictNotAcknowledgedException extends BusinessRuleViolationException {

    public ConflictNotAcknowledgedException(SyncRecordId id) {
        super("Synchronisation record " + id + " has no current acknowledgement for its present conflict");
    }
}
