package com.klabis.sync.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.sync.SyncRecordId;

/**
 * A {@link SynchronizationPort#reset} was called on a record that is not currently
 * terminally failed (design.md D10).
 */
public class SyncRecordNotFailedException extends BusinessRuleViolationException {

    public SyncRecordNotFailedException(SyncRecordId id) {
        super("Synchronisation record " + id + " is not terminally failed");
    }
}
