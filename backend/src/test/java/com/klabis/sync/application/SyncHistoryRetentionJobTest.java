package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import com.klabis.sync.fixtures.MutableClock;
import com.klabis.sync.fixtures.TestAdapterConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the history retention cleanup job (tasks.md 5.5): deletes {@code sync_attempt}
 * rows older than {@code history-retention}, never touches {@code sync_record}, and
 * leaves the record's last-successful-sync information intact.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, TestAdapterConfiguration.class, SyncHistoryRetentionJobTest.FixedClockConfiguration.class})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@DisplayName("SyncHistoryRetentionJob")
class SyncHistoryRetentionJobTest {

    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        Clock clock() {
            return new MutableClock(NOW, ZoneId.of("UTC"));
        }
    }

    @Autowired
    private SyncHistoryRetentionJob job;

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private SyncAttemptRepository syncAttemptRepository;

    @Autowired
    private SyncProjectionHasher hasher;

    @Test
    @DisplayName("removes only expired attempt rows, leaves sync_record and its last-success info intact")
    void removesOnlyExpiredAttempts() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, "retention-1"), new ExternalReference(ExternalSystem.ORIS, "8900"));
        SyncSnapshot agreed = SyncSnapshot.of(new com.klabis.sync.fixtures.TestSyncProjection("Sprint", "Brno"), hasher);
        record.recordSuccess(SyncDirection.INWARD, agreed, agreed, NOW);
        SyncRecord saved = syncRecordRepository.save(record);
        Instant lastSuccess = saved.getLastSuccessfulSyncAt();

        appendAttemptStartedAt(saved.getId(), NOW.minus(40, ChronoUnit.DAYS));
        appendAttemptStartedAt(saved.getId(), NOW.minus(1, ChronoUnit.DAYS));

        int deleted = job.cleanupExpiredAttempts();

        assertThat(deleted).isEqualTo(1);
        List<SyncAttempt> remaining = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(saved.getId());
        assertThat(remaining).hasSize(1);

        // H2's TIMESTAMP column rounds sub-millisecond precision on write rather than
        // truncating it, so comparing at microsecond precision can still differ by 1µs
        // on a rounding boundary — millisecond precision is comfortably coarser than
        // any such rounding and is all this assertion needs (the value round-tripped,
        // not any particular sub-millisecond digit).
        SyncRecord stillThere = syncRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(stillThere.getLastSuccessfulSyncAt().truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(lastSuccess.truncatedTo(ChronoUnit.MILLIS));
    }

    private void appendAttemptStartedAt(SyncRecordId recordId, Instant startedAt) {
        syncAttemptRepository.save(SyncAttempt.record(
                recordId, startedAt, SyncTriggerKind.SCHEDULED, null, SyncOutcome.SUCCESS,
                SyncHash.of("h"), SyncHash.of("h"), null, null));
    }
}
