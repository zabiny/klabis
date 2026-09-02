package com.klabis.sync.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncStatus;

/**
 * A manual {@link SynchronizationPort#synchronizeNow} was called on a record that is
 * {@code CONFLICT} or {@code FAILED} — it needs a decision (resolve or reset) before
 * an ordinary pass can run again. Without this guard, a manual trigger would claim
 * and re-evaluate the record via the normal decision table, silently returning a
 * terminally failed record to service without the {@code RESET} attempt row D10
 * requires, or writing over a standing conflict outside the acknowledge/resolve
 * workflow (design.md D6, D7, D10).
 */
public class SyncRecordNeedsResolutionException extends BusinessRuleViolationException {

    public SyncRecordNeedsResolutionException(SyncRecordId id, SyncStatus status) {
        super("Synchronisation record " + id + " is " + status + " and needs a decision (resolve the conflict or reset) before it can be synchronised again");
    }
}
