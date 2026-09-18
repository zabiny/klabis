package com.klabis.sync.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.sync.SyncRecordId;

/**
 * An operation that requires a standing conflict (acknowledge, resolve) was called on
 * a record that is not currently in conflict — either it never was, or a later pass
 * already cleared it (design.md D7).
 */
public class SyncRecordNotInConflictException extends BusinessRuleViolationException {

    public SyncRecordNotInConflictException(SyncRecordId id) {
        super("Synchronisation record " + id + " is not in conflict");
    }
}
