package com.klabis.sync.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import com.klabis.sync.fixtures.TestSyncProjection;
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
    private SyncScheduleRepository syncScheduleRepository;

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
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());

            SyncRecord saved = syncRecordRepository.save(record);
            Optional<SyncRecord> loaded = syncRecordRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getLocal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
            assertThat(loaded.get().getExternal().projection()).isEqualTo(new TestSyncProjection("Sprint", "Brno"));
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
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Secret Event Name", "Location X"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());

            SyncRecord saved = syncRecordRepository.save(record);

            String rawColumn = jdbcTemplate.queryForObject(
                    "SELECT local_projection FROM sync.sync_record WHERE id = ?",
                    String.class, saved.getId().value());

            assertThat(rawColumn).doesNotContain("Secret Event Name");
        }

        @Test
        @DisplayName("ciphertext differs between two saves of an identical projection, hash column does not")
        void ciphertextDiffersButHashDoesNot() {
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Same Name", "Same Location"), hasher);

            SyncRecord recordA = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
            recordA.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            SyncRecord savedA = syncRecordRepository.save(recordA);

            SyncRecord recordB = SyncRecord.enroll(SyncRecordId.newId(),
                    new SyncTarget(SyncEntityType.EVENT, "event-2"),
                    new ExternalReference(ExternalSystem.ORIS, "8124"));
            recordB.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
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
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            inSync.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            syncRecordRepository.save(inSync);

            SyncRecord retired = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "active-2"),
                    new ExternalReference(ExternalSystem.ORIS, "8502"));
            retired.retire(Instant.now());
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
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            failed.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            failed.recordTerminalFailure(5, "boom", Instant.now());
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
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            failed.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            failed.recordTerminalFailure(5, "boom", Instant.now());
            SyncRecord savedFailed = syncRecordRepository.save(failed);

            List<SyncRecord> nonRetired = syncRecordRepository.findAllNonRetired();

            assertThat(nonRetired).extracting(SyncRecord::getId).contains(savedFailed.getId());
        }

        @Test
        @DisplayName("excludes a retired record")
        void excludesRetiredRecord() {
            SyncRecord retired = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "nonretired-2"),
                    new ExternalReference(ExternalSystem.ORIS, "8512"));
            retired.retire(Instant.now());
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
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            dirty.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            SyncRecord saved = syncRecordRepository.save(dirty);
            syncScheduleRepository.apply(saved.getId(), ScheduleEffect.dirtySince(Instant.now()));

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).contains(dirty.getId());
        }

        @Test
        @DisplayName("picks up a record whose retry is due")
        void picksUpRetryDueRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-2"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            SyncRecord saved = syncRecordRepository.save(record);
            ScheduleEffect scheduleEffect = record.recordRetryableFailure(Instant.now().minus(Duration.ofMinutes(1)));
            syncScheduleRepository.apply(saved.getId(), scheduleEffect);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).contains(record.getId());
        }

        @Test
        @DisplayName("skips a record that is neither dirty nor due")
        void skipsRecordNotDueYet() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-3"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            record.recordRetryableFailure(Instant.now().plus(Duration.ofHours(1)));
            syncRecordRepository.save(record);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a retired record even if dirty")
        void skipsRetiredRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-4"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            SyncRecord saved = syncRecordRepository.save(record);
            syncScheduleRepository.apply(saved.getId(), ScheduleEffect.dirtySince(Instant.now()));
            SyncRecord retired = syncRecordRepository.findById(saved.getId()).orElseThrow();
            retired.retire(Instant.now());
            syncRecordRepository.save(retired);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a conflicted record — a standing conflict clears dirtySince/nextAttemptDueAt (design.md D7)")
        void skipsConflictedRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-5"), EXTERNAL_REF);
            SyncSnapshot local = SyncSnapshot.of(new TestSyncProjection("Local", "Brno"), hasher);
            SyncSnapshot external = SyncSnapshot.of(new TestSyncProjection("External", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, local, local, Instant.now());
            record.recordConflict(local, external, null, Instant.now());
            syncRecordRepository.save(record);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a conflicted record even when a later edit marked it dirty — markDirty has no status guard and re-sets dirtySince")
        void skipsConflictedRecordThatWasMarkedDirty() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-9"), EXTERNAL_REF);
            SyncSnapshot local = SyncSnapshot.of(new TestSyncProjection("Local", "Brno"), hasher);
            SyncSnapshot external = SyncSnapshot.of(new TestSyncProjection("External", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, local, local, Instant.now());
            record.recordConflict(local, external, null, Instant.now());
            SyncRecord saved = syncRecordRepository.save(record);
            syncScheduleRepository.apply(saved.getId(), ScheduleEffect.dirtySince(Instant.now()));

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM sync.sync_record WHERE id = ?", String.class, record.getId().value());
            Instant dirtySince = jdbcTemplate.queryForObject(
                    "SELECT dirty_since FROM sync.sync_schedule WHERE sync_record_id = ?", Instant.class, record.getId().value());
            assertThat(status).isEqualTo("CONFLICT");
            assertThat(dirtySince).as("fixture must actually reproduce the gap: a CONFLICT record with dirty_since set").isNotNull();

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a terminally failed record")
        void skipsTerminallyFailedRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-6"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            record.recordTerminalFailure(5, "boom", Instant.now());
            syncRecordRepository.save(record);

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a terminally failed record even when it was marked dirty by a later edit — recordTerminalFailure leaves dirtySince set")
        void skipsTerminallyFailedRecordThatWasMarkedDirty() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-8"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            record.recordTerminalFailure(5, "boom", Instant.now());
            SyncRecord saved = syncRecordRepository.save(record);
            syncScheduleRepository.apply(saved.getId(), ScheduleEffect.dirtySince(Instant.now()));

            String status = jdbcTemplate.queryForObject(
                    "SELECT status FROM sync.sync_record WHERE id = ?", String.class, record.getId().value());
            Instant dirtySince = jdbcTemplate.queryForObject(
                    "SELECT dirty_since FROM sync.sync_schedule WHERE sync_record_id = ?", Instant.class, record.getId().value());
            assertThat(status).isEqualTo("FAILED");
            assertThat(dirtySince).as("fixture must actually reproduce the gap: a FAILED record with dirty_since set").isNotNull();

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));

            assertThat(due).extracting(SyncRecord::getId).doesNotContain(record.getId());
        }

        @Test
        @DisplayName("skips a record with a fresh claim, picks it up once the claim lease expires")
        void skipsFreshlyClaimedRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-7"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            SyncRecord saved = syncRecordRepository.save(record);
            syncScheduleRepository.apply(saved.getId(), ScheduleEffect.dirtySince(Instant.now()));
            SyncRecord claimedRecord = syncRecordRepository.findById(saved.getId()).orElseThrow();
            claimedRecord.claim(Instant.now());
            syncRecordRepository.save(claimedRecord);

            List<SyncRecord> stillClaimed = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));
            assertThat(stillClaimed).extracting(SyncRecord::getId).doesNotContain(record.getId());

            List<SyncRecord> afterLeaseExpired = syncRecordRepository.findDueForScan(Instant.now().plus(Duration.ofMinutes(10)), Duration.ofMinutes(5));
            assertThat(afterLeaseExpired).extracting(SyncRecord::getId).contains(record.getId());
        }

        /**
         * Proposal.md task 4b.1's explicit trap: a {@code sync_record} row with no
         * matching {@code sync_schedule} row (e.g. one enrolled before this change, or
         * any other path that reaches {@code sync_record} without one) must still be
         * reachable by the query — an {@code INNER JOIN} would silently drop it from
         * every result the scan could ever produce, not merely from today's due
         * predicate, with no error anywhere. Verified directly against the join, not
         * through {@code findDueForScan}'s own predicate: a schedule-less record is
         * never "due" by definition either way (its {@code dirty_since}/
         * {@code next_attempt_due_at} are both absent), so the two join types would
         * agree on that outcome even if one of them had silently dropped the row —
         * this asserts the row is still present in the joined result set at all.
         */
        @Test
        @DisplayName("a record with no sync_schedule row is still reachable by the join, not dropped from the result set")
        void recordWithNoScheduleRowIsStillReachableByTheJoin() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-10"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            SyncRecord saved = syncRecordRepository.save(record);
            jdbcTemplate.update("DELETE FROM sync.sync_schedule WHERE sync_record_id = ?", saved.getId().value());

            Integer matchedRows = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM sync.sync_record sr
                    LEFT JOIN sync.sync_schedule ss ON ss.sync_record_id = sr.id
                    WHERE sr.id = ?
                    """, Integer.class, saved.getId().value());
            assertThat(matchedRows).as("an INNER JOIN would have produced 0 rows here instead of 1").isEqualTo(1);

            // Once something schedules an effect, the row exists and the record
            // behaves normally again (proposal.md task 2.6/4.7's invariant restored).
            syncScheduleRepository.apply(saved.getId(), ScheduleEffect.dirtySince(Instant.now()));
            List<SyncRecord> dueAfterMarkedDirty = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));
            assertThat(dueAfterMarkedDirty).extracting(SyncRecord::getId).contains(record.getId());
        }

        /**
         * Replaces the 4.10/4.11 double-write agreement test (proposal.md task 4b.4):
         * with the double-write gone, {@code sync_schedule} is the only store left —
         * this proves a pass that schedules a retry actually persists it there and
         * that {@code findDueForScan} genuinely reads it back, not merely that the
         * two former stores agreed with each other.
         */
        @Test
        @DisplayName("a persisted retry (sync_schedule alone) is picked up by the due scan")
        void persistedRetryIsPickedUpFromScheduleAlone() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "due-11"), EXTERNAL_REF);
            SyncSnapshot agreed = SyncSnapshot.of(new TestSyncProjection("Sprint", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, agreed, agreed, Instant.now());
            SyncRecord saved = syncRecordRepository.save(record);

            ScheduleEffect scheduleEffect = record.recordRetryableFailure(Instant.now().minus(Duration.ofMinutes(1)));
            syncScheduleRepository.apply(saved.getId(), scheduleEffect);

            SyncSchedule persisted = syncScheduleRepository.findByRecordId(saved.getId());
            assertThat(persisted.nextAttemptDueAt()).as("the retry must actually be persisted, not just returned in memory").isNotNull();

            List<SyncRecord> due = syncRecordRepository.findDueForScan(Instant.now(), Duration.ofMinutes(5));
            assertThat(due).extracting(SyncRecord::getId).contains(record.getId());
        }
    }
}
