package com.klabis.sync.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.ScheduleEffect;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncRecordRepository;
import com.klabis.sync.domain.SyncSchedule;
import com.klabis.sync.domain.SyncScheduleRepository;
import com.klabis.sync.domain.SyncTarget;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SyncScheduleRepositoryAdapter} (proposal.md task 2.2, 2.5, 2.6) — the new
 * {@code sync_schedule} table, purely additive at this point: nothing in production
 * code yet reads or writes it, so this test is the only thing exercising it.
 */
@DisplayName("SyncSchedule JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Import(SyncProjectionTypeTestConfiguration.class)
class SyncScheduleRepositoryAdapterTest {

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private SyncScheduleRepository syncScheduleRepository;

    private SyncRecordId enrollRecord(String entityId, String externalId) {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, entityId),
                new ExternalReference(ExternalSystem.ORIS, externalId));
        return syncRecordRepository.save(record).getId();
    }

    @Nested
    @DisplayName("findByRecordId()")
    class FindByRecordId {

        @Test
        @DisplayName("returns an empty schedule for a record with no schedule row (task 2.6 coded default)")
        void returnsEmptyScheduleWhenNoRowExists() {
            SyncRecordId recordId = enrollRecord("schedule-1", "9301");

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);

            assertThat(schedule).isEqualTo(SyncSchedule.empty());
        }
    }

    @Nested
    @DisplayName("createFor()")
    class CreateFor {

        @Test
        @DisplayName("creates an empty schedule row for a newly enrolled record")
        void createsEmptyScheduleRow() {
            SyncRecordId recordId = enrollRecord("schedule-2", "9302");

            syncScheduleRepository.createFor(recordId);

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince()).isNull();
            assertThat(schedule.nextAttemptDueAt()).isNull();
        }
    }

    @Nested
    @DisplayName("apply()")
    class Apply {

        @Test
        @DisplayName("dirtySince(instant) sets dirtySince on a freshly created schedule row")
        void appliesDirtySinceEffect() {
            SyncRecordId recordId = enrollRecord("schedule-3", "9303");
            syncScheduleRepository.createFor(recordId);
            Instant now = Instant.now();

            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(now));

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(now.truncatedTo(ChronoUnit.MILLIS));
            assertThat(schedule.nextAttemptDueAt()).isNull();
        }

        @Test
        @DisplayName("apply() works even without a prior createFor() call — falls back to the empty default before applying")
        void appliesEffectWithoutPriorCreate() {
            SyncRecordId recordId = enrollRecord("schedule-4", "9304");
            Instant dueAt = Instant.now().plusSeconds(900);

            syncScheduleRepository.apply(recordId, ScheduleEffect.dueAt(dueAt));

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.nextAttemptDueAt().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(dueAt.truncatedTo(ChronoUnit.MILLIS));
        }

        @Test
        @DisplayName("clear() clears both fields after dirtySince and dueAt were both set")
        void clearClearsBothFields() {
            SyncRecordId recordId = enrollRecord("schedule-5", "9305");
            syncScheduleRepository.createFor(recordId);
            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(Instant.now()));
            syncScheduleRepository.apply(recordId, ScheduleEffect.dueAt(Instant.now().plusSeconds(60)));

            syncScheduleRepository.apply(recordId, ScheduleEffect.clear());

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule).isEqualTo(SyncSchedule.empty());
        }

        @Test
        @DisplayName("clearDueAt() clears only nextAttemptDueAt, leaving dirtySince standing")
        void clearDueAtLeavesDirtySinceStanding() {
            SyncRecordId recordId = enrollRecord("schedule-6", "9306");
            syncScheduleRepository.createFor(recordId);
            Instant dirtySince = Instant.now();
            syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(dirtySince));
            syncScheduleRepository.apply(recordId, ScheduleEffect.dueAt(Instant.now().plusSeconds(60)));

            syncScheduleRepository.apply(recordId, ScheduleEffect.clearDueAt());

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince().truncatedTo(ChronoUnit.MILLIS)).isEqualTo(dirtySince.truncatedTo(ChronoUnit.MILLIS));
            assertThat(schedule.nextAttemptDueAt()).isNull();
        }

        @Test
        @DisplayName("repeated applies persist across separate calls (read-modify-write, no version conflict)")
        void repeatedAppliesPersistAcrossCalls() {
            SyncRecordId recordId = enrollRecord("schedule-7", "9307");
            syncScheduleRepository.createFor(recordId);

            for (int i = 0; i < 5; i++) {
                syncScheduleRepository.apply(recordId, ScheduleEffect.dirtySince(Instant.now()));
            }

            SyncSchedule schedule = syncScheduleRepository.findByRecordId(recordId);
            assertThat(schedule.dirtySince()).isNotNull();
        }
    }
}
