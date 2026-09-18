package com.klabis.sync.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.sync.SyncRecordId;

/**
 * A pass tried to claim a record whose claim lease has not yet expired (design.md
 * D12): a concurrent pass — scheduled or manual — is already working on it. The
 * record is skipped; other records remain available.
 */
public class SyncRecordClaimedException extends BusinessRuleViolationException {

    public SyncRecordClaimedException(SyncRecordId id) {
        super("Synchronisation record " + id + " is claimed by another pass");
    }
}
