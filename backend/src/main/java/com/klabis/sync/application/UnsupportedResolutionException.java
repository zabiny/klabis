package com.klabis.sync.application;

import com.klabis.common.exceptions.ConflictException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncResolution;

import java.util.Map;

public class UnsupportedResolutionException extends ConflictException implements UserFacingError {

    private final SyncResolution resolution;

    public UnsupportedResolutionException(SyncRecordId id, SyncResolution resolution) {
        super("Synchronisation record " + id + " cannot be resolved with " + resolution + ": the integration does not support it");
        this.resolution = resolution;
    }

    @Override
    public Map<String, Object> messageParameters() {
        return Map.of("resolution", resolution.name());
    }
}
