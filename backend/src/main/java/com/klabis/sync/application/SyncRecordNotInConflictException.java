package com.klabis.sync.application;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.sync.SyncRecordId;

/**
 * An operation that requires a standing conflict (acknowledge, resolve) was called on
 * a record that is not currently in conflict — either it never was, or a later pass
 * already cleared it (design.md D7).
 */
public class SyncRecordNotInConflictException extends ConflictException implements UserFacingError {

    public SyncRecordNotInConflictException(SyncRecordId id) {
        super("Synchronisation record " + id + " is not in conflict");
    }
}
