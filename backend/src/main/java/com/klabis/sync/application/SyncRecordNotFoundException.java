package com.klabis.sync.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.sync.SyncRecordId;

public class SyncRecordNotFoundException extends ResourceNotFoundException {

    public SyncRecordNotFoundException(SyncRecordId id) {
        super("Synchronisation record not found with ID: " + id);
    }
}
