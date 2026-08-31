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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

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
}
