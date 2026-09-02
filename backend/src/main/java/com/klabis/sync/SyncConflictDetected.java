package com.klabis.sync;

import com.klabis.sync.domain.SyncDirection;
import com.klabis.sync.domain.SyncHash;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.event.annotation.DomainEvent;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a pass finds a standing conflict (design.md D15): work stops and
 * nobody has been told yet. Carries identifiers, direction and hashes only — never
 * projections, so that no payload data reaches the retained {@code event_publication}
 * table.
 * <p>
 * Nothing consumes this event yet (design.md D8) — a conflicted record is discovered
 * by looking at that specific entity's synchronisation state until a consumer, or a
 * {@code syncStatus} filter on entity lists, is added.
 */
@RecordBuilder
@DomainEvent
public record SyncConflictDetected(
        UUID occurrenceId,
        SyncRecordId recordId,
        SyncDirection attemptedDirection,
        SyncHash localHash,
        SyncHash externalHash,
        Instant occurredAt
) {

    public SyncConflictDetected {
        Assert.notNull(occurrenceId, "occurrenceId is required");
        Assert.notNull(recordId, "recordId is required");
        Assert.notNull(localHash, "localHash is required");
        Assert.notNull(externalHash, "externalHash is required");
        Assert.notNull(occurredAt, "occurredAt is required");
    }

    public static SyncConflictDetected of(SyncRecordId recordId, SyncDirection attemptedDirection, SyncHash localHash, SyncHash externalHash) {
        return new SyncConflictDetected(UUID.randomUUID(), recordId, attemptedDirection, localHash, externalHash, Instant.now());
    }
}
