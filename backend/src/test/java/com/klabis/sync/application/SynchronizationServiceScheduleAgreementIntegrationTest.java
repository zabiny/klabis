package com.klabis.sync.application;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.sync.domain.*;
import com.klabis.sync.fixtures.TestAdapterConfiguration;
import com.klabis.sync.fixtures.TestSyncProjection;
import com.klabis.sync.fixtures.TestSynchronizationAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves iteration 4a is safe to ship on its own (proposal.md task 4.11): {@code
 * sync_schedule} is the authority for writes, but {@code sync_record}'s own {@code
 * dirty_since}/{@code next_attempt_due_at} columns still back every read
 * ({@code findDueForScan}, {@code findAllActive}, {@code SyncRecordMemento}) until
 * task 4b moves them over — so a pass that schedules a retry must leave both stores
 * agreeing on the same {@code nextAttemptDueAt}, or {@code findDueForScan} and a
 * direct read of the aggregate would silently disagree about when a record is next
 * due. Deleted in task 4b.4 once {@code sync_record}'s scheduling columns are gone
 * and there is only one store left to agree with itself.
 */
@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({TestApplicationConfiguration.class, TestAdapterConfiguration.class})
@DisplayName("sync_schedule and sync_record agree on scheduling (proposal.md task 4.10/4.11)")
class SynchronizationServiceScheduleAgreementIntegrationTest {

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private SynchronizationAdapter synchronizationAdapter;

    @Autowired
    private SyncRecordRepository syncRecordRepository;

    @Autowired
    private SyncScheduleRepository syncScheduleRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private TestSynchronizationAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = (TestSynchronizationAdapter) synchronizationAdapter;
        adapter.reset();
        adapter.withCapabilities(SyncCapabilities.bidirectional());
        circuitBreakerRegistry.circuitBreaker(ResilientAdapterExecutor.INSTANCE_NAME).reset();
    }

    @Test
    @DisplayName("a pass that schedules a retry leaves sync_schedule and sync_record agreeing on nextAttemptDueAt")
    void scheduleAndRecordAgreeAfterRetryableFailure() {
        adapter.withExternalState("9300", new TestSyncProjection("Sprint", "Brno"));
        adapter.withLocalState("event-agree-1", new TestSyncProjection("(unset)", "(unset)"));
        SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-agree-1");
        ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9300");
        SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
        synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        adapter.failNextReadExternalWith(3, new RetryableSyncFailureException("HTTP 503"));
        SyncRecord afterFailure = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        assertThat(afterFailure.getStatus()).isEqualTo(SyncStatus.RETRYING);
        assertThat(afterFailure.getNextAttemptDueAt()).isNotNull();

        SyncSchedule persistedSchedule = syncScheduleRepository.findByRecordId(enrolled.getId());
        SyncRecord persistedRecord = syncRecordRepository.findById(enrolled.getId()).orElseThrow();

        // H2 stores microsecond precision; Instant.now() carries nanoseconds. Truncate
        // both sides — the two stores genuinely agree, this is a storage artefact, not
        // a real disagreement.
        assertThat(persistedSchedule.nextAttemptDueAt().truncatedTo(ChronoUnit.MILLIS))
                .as("sync_schedule.next_attempt_due_at must agree with sync_record.next_attempt_due_at (task 4.10 double-write)")
                .isEqualTo(persistedRecord.getNextAttemptDueAt().truncatedTo(ChronoUnit.MILLIS))
                .isEqualTo(afterFailure.getNextAttemptDueAt().truncatedTo(ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("markDirty leaves sync_schedule and sync_record agreeing on dirtySince")
    void scheduleAndRecordAgreeAfterMarkDirty() {
        adapter.withExternalState("9301", new TestSyncProjection("Sprint", "Brno"));
        adapter.withLocalState("event-agree-2", new TestSyncProjection("Sprint", "Brno"));
        SyncTarget target = new SyncTarget(SyncEntityType.EVENT, "event-agree-2");
        ExternalReference externalRef = new ExternalReference(ExternalSystem.ORIS, "9301");
        SyncRecord enrolled = synchronizationPort.enroll(target, externalRef);
        synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        synchronizationPort.markDirty(target);

        SyncSchedule persistedSchedule = syncScheduleRepository.findByRecordId(enrolled.getId());
        SyncRecord persistedRecord = syncRecordRepository.findById(enrolled.getId()).orElseThrow();

        assertThat(persistedSchedule.dirtySince())
                .isNotNull();
        assertThat(persistedSchedule.dirtySince().truncatedTo(ChronoUnit.MILLIS))
                .as("sync_schedule.dirty_since must agree with sync_record.dirty_since (task 4.10 double-write)")
                .isEqualTo(persistedRecord.getDirtySince().truncatedTo(ChronoUnit.MILLIS));
    }
}
