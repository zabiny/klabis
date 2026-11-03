package com.klabis.sync.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.SyncPairing;
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

import java.time.Instant;
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
    @DisplayName("findBySystemAndExternalId() — looks a pairing up from the external side (design.md, \"Domain Changes\")")
    class FindBySystemAndExternalId {

        @Test
        @DisplayName("finds an active record")
        void findsActiveRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "ext-active"),
                    new ExternalReference(ExternalSystem.ORIS, "9001"));
            syncRecordRepository.save(record);

            Optional<SyncRecord> found = syncRecordRepository.findBySystemAndExternalId(ExternalSystem.ORIS, "9001");

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(record.getId());
        }

        @Test
        @DisplayName("finds a conflicted record")
        void findsConflictedRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "ext-conflicted"),
                    new ExternalReference(ExternalSystem.ORIS, "9002"));
            SyncSnapshot local = SyncSnapshot.of(new TestSyncProjection("Local", "Brno"), hasher);
            SyncSnapshot external = SyncSnapshot.of(new TestSyncProjection("External", "Brno"), hasher);
            record.recordSuccess(SyncDirection.INWARD, local, local, Instant.now());
            record.recordConflict(local, external, null, Instant.now());
            syncRecordRepository.save(record);

            Optional<SyncRecord> found = syncRecordRepository.findBySystemAndExternalId(ExternalSystem.ORIS, "9002");

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(SyncStatus.CONFLICT);
        }

        @Test
        @DisplayName("finds a terminally failed record")
        void findsFailedRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "ext-failed"),
                    new ExternalReference(ExternalSystem.ORIS, "9003"));
            record.recordRetryableFailure(Instant.now());
            record.recordTerminalFailure(5, "boom", Instant.now());
            syncRecordRepository.save(record);

            Optional<SyncRecord> found = syncRecordRepository.findBySystemAndExternalId(ExternalSystem.ORIS, "9003");

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(SyncStatus.FAILED);
        }

        /**
         * Retired must be included — a repeat import of a finished event's pairing
         * needs to find it in order to reactivate it (design.md D7); omitting retired
         * pairings here is the easy mistake this test exists to catch, and the whole
         * of a later reactivation slice depends on this lookup seeing them.
         */
        @Test
        @DisplayName("finds a retired record")
        void findsRetiredRecord() {
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), new SyncTarget(SyncEntityType.EVENT, "ext-retired"),
                    new ExternalReference(ExternalSystem.ORIS, "9004"));
            record.retire(Instant.now());
            syncRecordRepository.save(record);

            Optional<SyncRecord> found = syncRecordRepository.findBySystemAndExternalId(ExternalSystem.ORIS, "9004");

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(SyncStatus.RETIRED);
        }

        @Test
        @DisplayName("returns empty when nothing is paired for the external reference")
        void returnsEmptyWhenNotPaired() {
            Optional<SyncRecord> found = syncRecordRepository.findBySystemAndExternalId(ExternalSystem.ORIS, "unknown-external-id");

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

            SyncRecord retired = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
            retired.retire(Instant.now());
            SyncRecord savedRetired = syncRecordRepository.save(retired);
            jdbcTemplate.update("UPDATE sync.sync_record SET external_system = 'OTHER' WHERE id = ?",
                    savedRetired.getId().value());
            syncRecordRepository.save(SyncRecord.enroll(SyncRecordId.newId(), TARGET,
                    new ExternalReference(ExternalSystem.ORIS, "9001")));

            assertThat(syncRecordRepository.pairingOf(TARGET)).isEqualTo(SyncPairing.ACTIVE);
        }
    }
}
