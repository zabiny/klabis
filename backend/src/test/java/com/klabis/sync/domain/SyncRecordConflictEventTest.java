package com.klabis.sync.domain;

import com.klabis.sync.SyncConflictDetected;
import com.klabis.sync.SyncRecordId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyncRecordConflictEventTest {

    private record TestProjection(String name) implements SyncProjection {
        @Override
        public SyncEntityType entityType() {
            return SyncEntityType.EVENT;
        }
    }

    private static final SyncProjectionHasher HASHER = projection -> SyncHash.of(String.valueOf(projection.hashCode()));
    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-1");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "8123");

    @Test
    void recordConflict_firstDetection_publishesEvent() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot local = SyncSnapshot.of(new TestProjection("local edit"), HASHER);
        SyncSnapshot external = SyncSnapshot.of(new TestProjection("external edit"), HASHER);

        record.recordConflict(local, external, null);

        assertThat(record.getDomainEvents()).hasSize(1);
        assertThat(record.getDomainEvents().get(0)).isInstanceOf(SyncConflictDetected.class);
    }

    @Test
    void recordConflict_sameCollisionAgain_doesNotRepublish() {
        // Simulates a resolution rejected twice against the same unmoved collision, or
        // a re-evaluation that lands back on the identical hash pair: the manager
        // already has one SyncConflictDetected for this situation and does not need a
        // second (design.md D15 — the event exists so a consumer can be added later;
        // repeating it for no new information defeats that purpose).
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot local = SyncSnapshot.of(new TestProjection("local edit"), HASHER);
        SyncSnapshot external = SyncSnapshot.of(new TestProjection("external edit"), HASHER);

        record.recordConflict(local, external, null);
        record.clearDomainEvents();

        record.recordConflict(local, external, null);

        assertThat(record.getDomainEvents()).isEmpty();
    }

    @Test
    void recordConflict_differentCollisionThanBefore_publishesAgain() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot local = SyncSnapshot.of(new TestProjection("local edit"), HASHER);
        SyncSnapshot external = SyncSnapshot.of(new TestProjection("external edit"), HASHER);
        record.recordConflict(local, external, null);
        record.clearDomainEvents();

        SyncSnapshot newExternal = SyncSnapshot.of(new TestProjection("yet another external edit"), HASHER);
        record.recordConflict(local, newExternal, null);

        assertThat(record.getDomainEvents()).hasSize(1);
    }
}
