package com.klabis.sync.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.AttemptFilter;
import com.klabis.sync.application.AttemptView;
import com.klabis.sync.application.RecordView;
import com.klabis.sync.application.SyncHistoryQuery;
import com.klabis.sync.domain.*;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SyncHistoryQuery JDBC adapter")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Import({SyncProjectionTypeTestConfiguration.class, SyncHistoryQueryAdapter.class})
class SyncHistoryQueryAdapterTest {

    private static final Instant BASE = Instant.parse("2026-01-01T10:00:00Z");

    @Autowired
    private SyncHistoryQuery query;

    @Autowired
    private SyncRecordRepository recordRepository;

    @Autowired
    private SyncAttemptRepository attemptRepository;

    private SyncRecordId eventRecord;
    private SyncRecordId memberRecord;

    @BeforeEach
    void setUp() {
        eventRecord = recordRepository.save(SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, "event-1"), new ExternalReference(ExternalSystem.ORIS, "8123"))).getId();
        memberRecord = recordRepository.save(SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.MEMBER, "member-1"), new ExternalReference(ExternalSystem.ORIS, "77"))).getId();
    }

    private void attempt(SyncRecordId record, int minutes, SyncTriggerKind trigger, SyncDirection direction,
                         SyncOutcome outcome) {
        attemptRepository.save(SyncAttempt.record(record, BASE.plus(minutes, ChronoUnit.MINUTES), trigger, direction,
                outcome, SyncHash.of("h"), SyncHash.of("h"), null, null));
    }

    @Test
    @DisplayName("lists attempts newest first with the owning entity and external id")
    void ordersNewestFirstAndJoinsRecord() {
        attempt(eventRecord, 1, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);
        attempt(memberRecord, 3, SyncTriggerKind.LOCAL_CHANGE, SyncDirection.OUTWARD, SyncOutcome.SUCCESS);
        attempt(eventRecord, 2, SyncTriggerKind.SCHEDULED, SyncDirection.INWARD, SyncOutcome.SUCCESS);

        Page<AttemptView> page = query.listAttempts(AttemptFilter.changedOnly(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(v -> v.attempt().getStartedAt())
                .containsExactly(BASE.plus(3, ChronoUnit.MINUTES), BASE.plus(2, ChronoUnit.MINUTES), BASE.plus(1, ChronoUnit.MINUTES));
        AttemptView newest = page.getContent().get(0);
        assertThat(newest.target()).isEqualTo(new SyncTarget(SyncEntityType.MEMBER, "member-1"));
        assertThat(newest.externalId()).isEqualTo("77");
        assertThat(newest.attempt().getDirection()).isEqualTo(SyncDirection.OUTWARD);
        assertThat(newest.attempt().getTrigger()).isEqualTo(SyncTriggerKind.LOCAL_CHANGE);
    }

    @Test
    @DisplayName("pages the result")
    void pagesResult() {
        for (int i = 0; i < 5; i++) {
            attempt(eventRecord, i, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);
        }

        Page<AttemptView> second = query.listAttempts(AttemptFilter.changedOnly(), PageRequest.of(1, 2));

        assertThat(second.getTotalElements()).isEqualTo(5);
        assertThat(second.getTotalPages()).isEqualTo(3);
        assertThat(second.getContent()).extracting(v -> v.attempt().getStartedAt())
                .containsExactly(BASE.plus(2, ChronoUnit.MINUTES), BASE.plus(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("hides scheduled direction-less successes, keeps manual ones and other outcomes")
    void hidesAttemptsThatChangedNothing() {
        attempt(eventRecord, 1, SyncTriggerKind.SCHEDULED, null, SyncOutcome.SUCCESS);
        attempt(eventRecord, 2, SyncTriggerKind.LOCAL_CHANGE, null, SyncOutcome.SUCCESS);
        attempt(eventRecord, 3, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);
        attempt(eventRecord, 4, SyncTriggerKind.SCHEDULED, null, SyncOutcome.OUTAGE);
        attempt(eventRecord, 5, SyncTriggerKind.SCHEDULED, SyncDirection.INWARD, SyncOutcome.SUCCESS);

        Page<AttemptView> changed = query.listAttempts(AttemptFilter.changedOnly(), PageRequest.of(0, 10));
        Page<AttemptView> all = query.listAttempts(AttemptFilter.all(), PageRequest.of(0, 10));

        assertThat(changed.getContent()).extracting(v -> v.attempt().getStartedAt())
                .containsExactly(BASE.plus(5, ChronoUnit.MINUTES), BASE.plus(4, ChronoUnit.MINUTES), BASE.plus(3, ChronoUnit.MINUTES));
        assertThat(all.getTotalElements()).isEqualTo(5);
    }

    private static java.util.List<Instant> startedAts(Page<AttemptView> page) {
        return page.getContent().stream().map(v -> v.attempt().getStartedAt()).toList();
    }

    @Test
    @DisplayName("filters by entity type")
    void filtersByEntityType() {
        attempt(eventRecord, 1, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);
        attempt(memberRecord, 2, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);

        Page<AttemptView> page = query.listAttempts(AttemptFilter.all().withEntityType(SyncEntityType.MEMBER), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).target().entityType()).isEqualTo(SyncEntityType.MEMBER);
    }

    @Test
    @DisplayName("filters by outcome")
    void filtersByOutcome() {
        attempt(eventRecord, 1, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);
        attempt(eventRecord, 2, SyncTriggerKind.MANUAL, null, SyncOutcome.CONFLICT);
        attempt(eventRecord, 3, SyncTriggerKind.MANUAL, null, SyncOutcome.FAILED);

        Page<AttemptView> page = query.listAttempts(AttemptFilter.all().withOutcome(SyncOutcome.CONFLICT), PageRequest.of(0, 10));

        assertThat(startedAts(page)).containsExactly(BASE.plus(2, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("filters by period with inclusive from and exclusive to")
    void filtersByPeriod() {
        for (int i = 1; i <= 4; i++) {
            attempt(eventRecord, i, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);
        }
        Instant from = BASE.plus(2, ChronoUnit.MINUTES);
        Instant to = BASE.plus(4, ChronoUnit.MINUTES);

        assertThat(startedAts(query.listAttempts(AttemptFilter.all().withFrom(from).withTo(to), PageRequest.of(0, 10))))
                .containsExactly(BASE.plus(3, ChronoUnit.MINUTES), from);
        assertThat(query.listAttempts(AttemptFilter.all().withFrom(from), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
        assertThat(query.listAttempts(AttemptFilter.all().withTo(to), PageRequest.of(0, 10)).getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("combines filters, including with the unchanged rule, and counts only matching rows")
    void combinesFilters() {
        attempt(eventRecord, 1, SyncTriggerKind.MANUAL, null, SyncOutcome.CONFLICT);
        attempt(memberRecord, 2, SyncTriggerKind.MANUAL, null, SyncOutcome.CONFLICT);
        attempt(eventRecord, 3, SyncTriggerKind.SCHEDULED, null, SyncOutcome.SUCCESS);
        attempt(eventRecord, 4, SyncTriggerKind.MANUAL, null, SyncOutcome.FAILED);

        AttemptFilter filter = AttemptFilter.changedOnly().withEntityType(SyncEntityType.EVENT).withOutcome(SyncOutcome.CONFLICT);
        Page<AttemptView> page = query.listAttempts(filter, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(startedAts(page)).containsExactly(BASE.plus(1, ChronoUnit.MINUTES));
        assertThat(query.listAttempts(AttemptFilter.changedOnly().withEntityType(SyncEntityType.EVENT).withOutcome(SyncOutcome.SUCCESS),
                PageRequest.of(0, 10)).getTotalElements()).isZero();
    }

    @Test
    @DisplayName("lists only the attempts of one record, newest first, hiding unchanged ones unless asked")
    void listsAttemptsOfOneRecord() {
        attempt(eventRecord, 1, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);
        attempt(eventRecord, 2, SyncTriggerKind.SCHEDULED, null, SyncOutcome.SUCCESS);
        attempt(eventRecord, 3, SyncTriggerKind.SCHEDULED, SyncDirection.INWARD, SyncOutcome.SUCCESS);
        attempt(memberRecord, 4, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);

        Page<AttemptView> changed = query.listAttemptsOf(eventRecord, false, PageRequest.of(0, 10));
        Page<AttemptView> all = query.listAttemptsOf(eventRecord, true, PageRequest.of(0, 10));

        assertThat(startedAts(changed)).containsExactly(BASE.plus(3, ChronoUnit.MINUTES), BASE.plus(1, ChronoUnit.MINUTES));
        assertThat(changed.getTotalElements()).isEqualTo(2);
        assertThat(startedAts(all)).containsExactly(
                BASE.plus(3, ChronoUnit.MINUTES), BASE.plus(2, ChronoUnit.MINUTES), BASE.plus(1, ChronoUnit.MINUTES));
        assertThat(all.getContent()).allSatisfy(v ->
                assertThat(v.target()).isEqualTo(new SyncTarget(SyncEntityType.EVENT, "event-1")));
    }

    @Test
    @DisplayName("pages the attempts of one record")
    void pagesAttemptsOfOneRecord() {
        for (int i = 0; i < 5; i++) {
            attempt(eventRecord, i, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);
        }
        attempt(memberRecord, 9, SyncTriggerKind.MANUAL, null, SyncOutcome.SUCCESS);

        Page<AttemptView> second = query.listAttemptsOf(eventRecord, true, PageRequest.of(1, 2));

        assertThat(second.getTotalElements()).isEqualTo(5);
        assertThat(startedAts(second)).containsExactly(BASE.plus(2, ChronoUnit.MINUTES), BASE.plus(1, ChronoUnit.MINUTES));
    }

    private SyncRecordId recordIn(SyncEntityType type, String entityId, String externalId, SyncStatus status, Instant lastSuccess) {
        return recordRepository.save(SyncRecord.reconstruct(SyncRecordId.newId(), new SyncTarget(type, entityId),
                new ExternalReference(ExternalSystem.ORIS, externalId), status, null, null, null, null,
                SyncSchedule.empty(), null, null, lastSuccess, null, null)).getId();
    }

    @Test
    @DisplayName("lists only records in the requested statuses")
    void listsRecordsInRequestedStatuses() {
        recordIn(SyncEntityType.EVENT, "e-conflict", "1", SyncStatus.CONFLICT, null);
        recordIn(SyncEntityType.EVENT, "e-failed", "2", SyncStatus.FAILED, null);
        recordIn(SyncEntityType.EVENT, "e-retrying", "3", SyncStatus.RETRYING, null);
        recordIn(SyncEntityType.EVENT, "e-retired", "4", SyncStatus.RETIRED, null);

        var attention = query.listRecordsIn(java.util.Set.of(SyncStatus.CONFLICT, SyncStatus.FAILED));
        var conflictOnly = query.listRecordsIn(java.util.Set.of(SyncStatus.CONFLICT));

        assertThat(attention).extracting(r -> r.target().entityId()).containsExactlyInAnyOrder("e-conflict", "e-failed");
        assertThat(attention).extracting(RecordView::status)
                .containsExactlyInAnyOrder(SyncStatus.CONFLICT, SyncStatus.FAILED);
        assertThat(conflictOnly).extracting(r -> r.target().entityId()).containsExactly("e-conflict");
        assertThat(query.listRecordsIn(java.util.Set.of())).isEmpty();
    }

    @Test
    @DisplayName("exposes external id and last successful sync")
    void exposesExternalIdAndLastSuccess() {
        recordIn(SyncEntityType.MEMBER, "m-1", "177", SyncStatus.CONFLICT, BASE);
        recordIn(SyncEntityType.MEMBER, "m-2", "178", SyncStatus.CONFLICT, null);

        var views = query.listRecordsIn(java.util.Set.of(SyncStatus.CONFLICT));

        assertThat(views).filteredOn(v -> v.target().entityId().equals("m-1")).singleElement().satisfies(v -> {
            assertThat(v.externalId()).isEqualTo("177");
            assertThat(v.lastSuccessfulSyncAt()).isEqualTo(BASE);
        });
        assertThat(views).filteredOn(v -> v.target().entityId().equals("m-2")).singleElement()
                .satisfies(v -> assertThat(v.lastSuccessfulSyncAt()).isNull());
    }

    @Test
    @DisplayName("counts failed attempts since the last success or reset, ignoring outages")
    void countsFailedAttemptsSinceLastSuccess() {
        SyncRecordId failed = recordIn(SyncEntityType.EVENT, "e-failed", "9", SyncStatus.FAILED, BASE);
        attempt(failed, 1, SyncTriggerKind.SCHEDULED, SyncDirection.INWARD, SyncOutcome.FAILED);
        attempt(failed, 2, SyncTriggerKind.SCHEDULED, SyncDirection.INWARD, SyncOutcome.SUCCESS);
        attempt(failed, 3, SyncTriggerKind.SCHEDULED, null, SyncOutcome.FAILED);
        attempt(failed, 4, SyncTriggerKind.SCHEDULED, null, SyncOutcome.OUTAGE);
        attempt(failed, 5, SyncTriggerKind.SCHEDULED, null, SyncOutcome.FAILED);
        SyncRecordId reset = recordIn(SyncEntityType.EVENT, "e-reset", "10", SyncStatus.CONFLICT, null);
        attempt(reset, 1, SyncTriggerKind.SCHEDULED, null, SyncOutcome.FAILED);
        attempt(reset, 2, SyncTriggerKind.MANUAL, null, SyncOutcome.RESET);
        attempt(reset, 3, SyncTriggerKind.SCHEDULED, null, SyncOutcome.CONFLICT);
        SyncRecordId neverAttempted = recordIn(SyncEntityType.EVENT, "e-none", "11", SyncStatus.CONFLICT, null);
        attempt(neverAttempted, 1, SyncTriggerKind.SCHEDULED, null, SyncOutcome.FAILED);
        attempt(neverAttempted, 2, SyncTriggerKind.SCHEDULED, null, SyncOutcome.FAILED);

        var views = query.listRecordsIn(java.util.Set.of(SyncStatus.CONFLICT, SyncStatus.FAILED));

        assertThat(views).extracting(v -> v.target().entityId() + "=" + v.failedAttemptsSinceLastSuccess())
                .containsExactlyInAnyOrder("e-failed=2", "e-reset=1", "e-none=2");
    }
}
