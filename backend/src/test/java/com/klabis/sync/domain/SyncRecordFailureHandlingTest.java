package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import com.klabis.sync.SyncTerminallyFailed;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SyncRecordFailureHandlingTest {

    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-1");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "8123");

    @Test
    void recordRetryableFailure_fromNew_entersRetrying() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);

        record.recordRetryableFailure(Instant.now().plus(15, ChronoUnit.MINUTES));

        assertThat(record.getStatus()).isEqualTo(SyncStatus.RETRYING);
        assertThat(record.getNextAttemptDueAt()).isNotNull();
    }

    @Test
    void recordRetryableFailure_fromInSync_entersRetrying() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncProjectionHasher hasher = projection -> SyncHash.of(String.valueOf(projection.hashCode()));
        SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("agreed"), hasher);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed);
        assertThat(record.getStatus()).isEqualTo(SyncStatus.IN_SYNC);

        record.recordRetryableFailure(Instant.now().plus(15, ChronoUnit.MINUTES));

        assertThat(record.getStatus()).isEqualTo(SyncStatus.RETRYING);
    }

    @Test
    void recordOutage_fromNew_entersRetryingWithInitialDelay() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        Instant initialDelay = Instant.now().plus(15, ChronoUnit.MINUTES);

        record.recordOutage(initialDelay);

        assertThat(record.getStatus()).isEqualTo(SyncStatus.RETRYING);
        assertThat(record.getNextAttemptDueAt()).isEqualTo(initialDelay);
    }

    @Test
    void recordTerminalFailure_movesToFailed_andPublishesEvent() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        record.recordRetryableFailure(Instant.now());

        record.recordTerminalFailure(5, "persistent failure");

        assertThat(record.getStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(record.getNextAttemptDueAt()).isNull();
        assertThat(record.getDomainEvents()).hasSize(1);
        assertThat(record.getDomainEvents().get(0)).isInstanceOf(SyncTerminallyFailed.class);
    }

    @Test
    void reset_fromFailed_returnsToInSync() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        record.recordRetryableFailure(Instant.now());
        record.recordTerminalFailure(5, "persistent failure");

        record.reset();

        assertThat(record.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(record.getNextAttemptDueAt()).isNull();
    }

    @Test
    void reset_fromNonFailedStatus_rejected() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);

        assertThatThrownBy(record::reset).isInstanceOf(IllegalStateException.class);
    }

    private record TestProjection(String name) implements SyncProjection {
        @Override
        public SyncEntityType entityType() {
            return SyncEntityType.EVENT;
        }
    }
}
