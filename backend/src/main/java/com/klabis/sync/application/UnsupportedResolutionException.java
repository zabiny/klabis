package com.klabis.sync.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncResolution;

/**
 * A resolution requested a direction the integration cannot perform — for an
 * inward-only integration, {@code OUTWARD} is never offered (design.md D6, REST API
 * section).
 */
public class UnsupportedResolutionException extends BusinessRuleViolationException {

    public UnsupportedResolutionException(SyncRecordId id, SyncResolution resolution) {
        super("Synchronisation record " + id + " cannot be resolved with " + resolution + ": the integration does not support it");
    }
}
