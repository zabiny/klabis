package com.klabis.sync.application;

import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * A record's current state for listings. {@code failedAttemptsSinceLastSuccess} follows the
 * retry rule: attempts since the latest {@code SUCCESS} or {@code RESET}, outages not counted.
 */
public record RecordView(
        SyncTarget target,
        String externalId,
        SyncStatus status,
        @Nullable Instant lastSuccessfulSyncAt,
        int failedAttemptsSinceLastSuccess) {
}
