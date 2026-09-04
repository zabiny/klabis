package com.klabis.sync.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.klabis.sync.infrastructure.jdbc.SyncProjectionTypeTestConfiguration.TestProjection;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SyncRecord JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Import(SyncProjectionTypeTestConfiguration.class)
class SyncRecordJdbcRepositoryTest {

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SyncProjectionHasher hasher;

    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-1");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "8123");

    @Nested
    @DisplayName("round-trip a record with all three snapshots")
    class RoundTrip {

        @Test
        @DisplayName("should persist and load a record with local, external and baseline snapshots")
        void shouldPersistAndLoadFullRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

            SyncRecord saved = syncRecordRepository.save(record);
            Optional<SyncRecord> loaded = syncRecordRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getLocal().projection()).isEqualTo(new TestProjection("Sprint", "Brno"));
            assertThat(loaded.get().getExternal().projection()).isEqualTo(new TestProjection("Sprint", "Brno"));
            assertThat(loaded.get().getBaseline().local().hash()).isEqualTo(agreed.hash());
            assertThat(loaded.get().getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        }

        @Test
        @DisplayName("should populate audit metadata after save")
        void shouldPopulateAuditMetadata() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);

            SyncRecord saved = syncRecordRepository.save(record);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("encryption at rest (design.md D13)")
    class EncryptionAtRest {

        @Test
        @DisplayName("projection column is not readable as plaintext in the database")
        void projectionColumnIsNotPlaintext() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Secret Event Name", "Location X"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed);

            SyncRecord saved = syncRecordRepository.save(record);

            String rawColumn = jdbcTemplate.queryForObject(
                    "SELECT local_projection FROM sync.sync_record WHERE id = ?",
                    String.class, saved.getId().value());

            assertThat(rawColumn).doesNotContain("Secret Event Name");
        }

        @Test
        @DisplayName("ciphertext differs between two saves of an identical projection, hash column does not")
        void ciphertextDiffersButHashDoesNot() {
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Same Name", "Same Location"), hasher);

            SyncRecord recordA = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
            recordA.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            SyncRecord savedA = syncRecordRepository.save(recordA);

            SyncRecord recordB = SyncRecord.enroll(SyncRecordId.newId(),
                    new SyncTarget(SyncEntityType.EVENT, "event-2"),
                    new ExternalReference(ExternalSystem.ORIS, "8124"));
            recordB.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            SyncRecord savedB = syncRecordRepository.save(recordB);

            String ciphertextA = jdbcTemplate.queryForObject(
                    "SELECT local_projection FROM sync.sync_record WHERE id = ?", String.class, savedA.getId().value());
            String ciphertextB = jdbcTemplate.queryForObject(
                    "SELECT local_projection FROM sync.sync_record WHERE id = ?", String.class, savedB.getId().value());
            String hashA = jdbcTemplate.queryForObject(
                    "SELECT local_hash FROM sync.sync_record WHERE id = ?", String.class, savedA.getId().value());
            String hashB = jdbcTemplate.queryForObject(
                    "SELECT local_hash FROM sync.sync_record WHERE id = ?", String.class, savedB.getId().value());

            assertThat(ciphertextA).isNotEqualTo(ciphertextB);
            assertThat(hashA).isEqualTo(hashB);
        }
    }

    @Nested
    @DisplayName("findByTargetAndSystem()")
    class FindByTargetAndSystem {

        @Test
        @DisplayName("should find an enrolled record by target and external system")
        void shouldFindByTargetAndSystem() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
            syncRecordRepository.save(record);

            Optional<SyncRecord> found = syncRecordRepository.findByTargetAndSystem(TARGET, ExternalSystem.ORIS);

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("should return empty when no record is enrolled for the target")
        void shouldReturnEmptyWhenNotEnrolled() {
            Optional<SyncRecord> found = syncRecordRepository.findByTargetAndSystem(
                    new SyncTarget(SyncEntityType.EVENT, "unknown-event"), ExternalSystem.ORIS);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllActive() — backs the nightly full pass (design.md D10, D17)")
    class FindAllActive {

        @Test
        @DisplayName("returns records in every non-retired status, excludes RETIRED")
        void returnsEveryNonRetiredRecord() {
            SyncRecord inSync = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "active-1"),
                    new ExternalReference(ExternalSystem.ORIS, "8501"));
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            inSync.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            syncRecordRepository.save(inSync);

            SyncRecord retired = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "active-2"),
                    new ExternalReference(ExternalSystem.ORIS, "8502"));
            retired.retire();
            SyncRecord savedRetired = syncRecordRepository.save(retired);

            List<SyncRecord> active = syncRecordRepository.findAllActive();

            assertThat(active).extracting(SyncRecord::getId).contains(inSync.getId());
            assertThat(active).extracting(SyncRecord::getId).doesNotContain(savedRetired.getId());
        }

        @Test
        @DisplayName("excludes a terminally failed record — runScheduledPass must never see one (design.md D10)")
        void excludesTerminallyFailedRecord() {
            SyncRecord failed = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "active-3"),
                    new ExternalReference(ExternalSystem.ORIS, "8503"));
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            failed.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            failed.recordTerminalFailure(5, "boom");
            SyncRecord savedFailed = syncRecordRepository.save(failed);

            List<SyncRecord> active = syncRecordRepository.findAllActive();

            assertThat(active).extracting(SyncRecord::getId).doesNotContain(savedFailed.getId());
        }
    }

    @Nested
    @DisplayName("findAllNonRetired() — backs the manual all-upcoming bulk pass (design.md D18)")
    class FindAllNonRetired {

        @Test
        @DisplayName("includes a terminally failed record, so it can be reported rather than silently dropped")
        void includesTerminallyFailedRecord() {
            SyncRecord failed = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "nonretired-1"),
                    new ExternalReference(ExternalSystem.ORIS, "8511"));
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            failed.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            failed.recordTerminalFailure(5, "boom");
            SyncRecord savedFailed = syncRecordRepository.save(failed);

            List<SyncRecord> nonRetired = syncRecordRepository.findAllNonRetired();

            assertThat(nonRetired).extracting(SyncRecord::getId).contains(savedFailed.getId());
        }

        @Test
        @DisplayName("excludes a retired record")
        void excludesRetiredRecord() {
            SyncRecord retired = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "nonretired-2"),
                    new ExternalReference(ExternalSystem.ORIS, "8512"));
            retired.retire();
            SyncRecord savedRetired = syncRecordRepository.save(retired);

            List<SyncRecord> nonRetired = syncRecordRepository.findAllNonRetired();

            assertThat(nonRetired).extracting(SyncRecord::getId).doesNotContain(savedRetired.getId());
        }
    }

    @Nested
    @DisplayName("findDueForScan() — backs the frequent due scan (design.md D10)")
    class FindDueForScan {

        @Test
        @DisplayName("picks up a dirty record")
        void picksUpDirtyRecord() {
            SyncRecord dirty = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-1"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            dirty.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            dirty.markDirty();
            syncRecordRepository.save(dirty);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).contains(dirty.getId());
        }

        @Test
        @DisplayName("picks up a record whose retry is due")
        void picksUpRetryDueRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-2"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            record.recordRetryableFailure(Instant.now().minus(Duration.ofMinutes(1)));
            syncRecordRepository.save(record);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).contains(record.getId());
        }

        @Test
        @DisplayName("skips a record that is neither dirty nor due")
        void skipsRecordNotDueYet() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-3"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            record.recordRetryableFailure(Instant.now().plus(Duration.ofHours(1)));
            syncRecordRepository.save(record);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a retired record even if dirty")
        void skipsRetiredRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-4"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            record.markDirty();
            record.retire();
            syncRecordRepository.save(record);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a conflicted record — a standing conflict clears dirtySince/nextAttemptDueAt (design.md D7)")
        void skipsConflictedRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-5"), EXTERNAL_REF);
            SyncSnapshot local = SyncSnapshot.of(new TestProjection("Local", "Brno"), hasher);
            SyncSnapshot external = SyncSnapshot.of(new TestProjection("External", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, local, local);
            record.recordConflict(local, external, null);
            syncRecordRepository.save(record);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a terminally failed record")
        void skipsTerminallyFailedRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-6"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            record.recordTerminalFailure(5, "boom");
            syncRecordRepository.save(record);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a record with a fresh claim, picks it up once the claim lease expires")
        void skipsFreshlyClaimedRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-7"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed);
            record.markDirty();
            record.claim(Instant.now());
            syncRecordRepository.save(record);

            List<SyncRecord> stillClaimed = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));
            assertThat(stillClaimed).extracting(SyncRecord::getId).doesNotContain(record.getId());

            List<SyncRecord> afterLeaseExpired = syncRecordRepository.findDueForScan(Instant.now().plus(Duration.ofMinutes(10)), Duration.ofMinutes(5));
            assertThat(afterLeaseExpired).extracting(SyncRecord::getId).contains(record.getId());
        }
    }
}
