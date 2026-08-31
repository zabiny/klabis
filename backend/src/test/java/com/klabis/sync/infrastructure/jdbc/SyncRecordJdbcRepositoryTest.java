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
}
