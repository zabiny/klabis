package com.klabis.sync.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncAttempt;
import com.klabis.sync.domain.SyncAttemptRepository;
import com.klabis.sync.domain.SyncHash;
import com.klabis.sync.domain.SyncOutcome;
import com.klabis.sync.domain.SyncRecordRepository;
import com.klabis.sync.domain.SyncTriggerKind;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SyncAttempt JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Import(SyncProjectionTypeTestConfiguration.class)
class SyncAttemptJdbcRepositoryTest {

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private SyncAttemptRepository syncAttemptRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should append and load attempts for a record, newest first")
    void shouldAppendAndLoadAttempts() {
        SyncRecordId recordId = syncRecordRepository.save(
                com.klabis.sync.domain.SyncRecord.enroll(
                        SyncRecordId.newId(),
                        new com.klabis.sync.domain.SyncTarget(com.klabis.sync.domain.SyncEntityType.EVENT, "event-x"),
                        new com.klabis.sync.domain.ExternalReference(com.klabis.sync.domain.ExternalSystem.ORIS, "999"))
        ).getId();

        SyncAttempt first = SyncAttempt.record(recordId, SyncTriggerKind.SCHEDULED, null, SyncOutcome.SUCCESS,
                SyncHash.of("hash-a"), SyncHash.of("hash-a"), null, null);
        syncAttemptRepository.save(first);

        SyncAttempt second = SyncAttempt.record(recordId, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS,
                SyncHash.of("hash-b"), SyncHash.of("hash-b"), null, "admin");
        syncAttemptRepository.save(second);

        List<SyncAttempt> history = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(recordId);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getActingUser()).isEqualTo("admin");
    }

    @Test
    @DisplayName("holds no projections, only hashes")
    void holdsNoProjections() {
        SyncRecordId recordId = syncRecordRepository.save(
                com.klabis.sync.domain.SyncRecord.enroll(
                        SyncRecordId.newId(),
                        new com.klabis.sync.domain.SyncTarget(com.klabis.sync.domain.SyncEntityType.EVENT, "event-y"),
                        new com.klabis.sync.domain.ExternalReference(com.klabis.sync.domain.ExternalSystem.ORIS, "998"))
        ).getId();

        SyncAttempt attempt = SyncAttempt.record(recordId, SyncTriggerKind.SCHEDULED, null, SyncOutcome.SUCCESS,
                SyncHash.of("hash-only"), SyncHash.of("hash-only"), null, null);
        SyncAttempt saved = syncAttemptRepository.save(attempt);

        assertThat(saved.getLocalHash().value()).isEqualTo("hash-only");
    }

    @Nested
    @DisplayName("deleteAttemptsStartedBefore() — backs the history retention job (design.md D19)")
    class DeleteAttemptsStartedBefore {

        @Test
        @DisplayName("deletes only attempt rows older than the cutoff, never the sync_record itself")
        void deletesOnlyExpiredAttempts() {
            SyncRecordId recordId = syncRecordRepository.save(
                    com.klabis.sync.domain.SyncRecord.enroll(
                            SyncRecordId.newId(),
                            new com.klabis.sync.domain.SyncTarget(com.klabis.sync.domain.SyncEntityType.EVENT, "event-retention"),
                            new com.klabis.sync.domain.ExternalReference(com.klabis.sync.domain.ExternalSystem.ORIS, "997"))
            ).getId();

            Instant old = Instant.now().minus(40, ChronoUnit.DAYS);
            Instant recent = Instant.now().minus(1, ChronoUnit.DAYS);
            insertAttemptStartedAt(recordId, old);
            insertAttemptStartedAt(recordId, recent);

            int deleted = syncAttemptRepository.deleteAttemptsStartedBefore(Instant.now().minus(30, ChronoUnit.DAYS));

            assertThat(deleted).isEqualTo(1);
            List<SyncAttempt> remaining = syncAttemptRepository.findByRecordIdOrderByStartedAtDesc(recordId);
            assertThat(remaining).hasSize(1);
            Integer recordCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sync.sync_record WHERE id = ?", Integer.class, recordId.value());
            assertThat(recordCount).isEqualTo(1);
        }

        private void insertAttemptStartedAt(SyncRecordId recordId, Instant startedAt) {
            jdbcTemplate.update(
                    "INSERT INTO sync.sync_attempt (id, sync_record_id, started_at, trigger, outcome, local_hash, external_hash) " +
                            "VALUES (?, ?, ?, 'SCHEDULED', 'SUCCESS', 'h', 'h')",
                    UUID.randomUUID(), recordId.value(), java.sql.Timestamp.from(startedAt));
        }
    }
}
