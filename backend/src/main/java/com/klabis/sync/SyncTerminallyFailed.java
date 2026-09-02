package com.klabis.sync;

import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a record reaches terminal failure (design.md D10, D15): retryable
 * attempts since the last success or reset reached {@code klabis.sync.max-attempts},
 * so the record stops being attempted and waits for a manual reset. Work stops and
 * nobody has been told yet. Carries identifiers and the failure reason only — never
 * projections.
 */
@RecordBuilder
@DomainEvent
public record SyncTerminallyFailed(
        UUID occurrenceId,
        SyncRecordId recordId,
        int failedAttempts,
        String failureReason,
        Instant occurredAt
) {

    public SyncTerminallyFailed {
        Assert.notNull(occurrenceId, "occurrenceId is required");
        Assert.notNull(recordId, "recordId is required");
        Assert.notNull(occurredAt, "occurredAt is required");
    }

    public static SyncTerminallyFailed of(SyncRecordId recordId, int failedAttempts, String failureReason) {
        return new SyncTerminallyFailed(UUID.randomUUID(), recordId, failedAttempts, failureReason, Instant.now());
    }
}
