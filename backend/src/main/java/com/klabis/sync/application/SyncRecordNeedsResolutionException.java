package com.klabis.sync.application;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncStatus;

import java.util.Map;

public class SyncRecordNeedsResolutionException extends ConflictException implements UserFacingError {

    private final SyncStatus status;

    public SyncRecordNeedsResolutionException(SyncRecordId id, SyncStatus status) {
        super("Synchronisation record " + id + " is " + status + " and needs a decision (resolve the conflict or reset) before it can be synchronised again");
        this.status = status;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("syncStatus", status.name());
    }
}
