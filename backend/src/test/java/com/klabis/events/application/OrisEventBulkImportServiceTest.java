package com.klabis.events.application;

import com.klabis.common.sync.DataSync;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncRecord;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventBulkImportService — importEventsFromOris()")
class OrisEventBulkImportServiceTest {

    @Mock
    private DataSync dataSync;

    @Mock
    private EventRepository eventRepository;

    private OrisEventBulkImportService service;

    @BeforeEach
    void setUp() {
        service = new OrisEventBulkImportService(dataSync, eventRepository);
    }

    private static SyncId externalId(int orisId) {
        return SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
    }

    private void stubSyncSuccess(int orisId, Event importedEvent) {
        SyncId localId = SyncId.localId(SyncType.EVENT, importedEvent.getId().value().toString());
        when(dataSync.sync(eq(externalId(orisId)), eq(DataSync.Direction.PULL)))
                .thenReturn(SyncRecord.success(localId, externalId(orisId)));
        when(eventRepository.findById(importedEvent.getId())).thenReturn(Optional.of(importedEvent));
    }

    private void stubSyncError(int orisId, String failureCause) {
        when(dataSync.sync(eq(externalId(orisId)), eq(DataSync.Direction.PULL)))
                .thenReturn(SyncRecord.failure(null, externalId(orisId), failureCause));
    }

    @Nested
    @DisplayName("importEventsFromOris()")
    class ImportEventsFromOris {

        @Test
        @DisplayName("should keep per-id independence: middle id fails with unique-constraint error, outer ids imported")
        void shouldImportThreeEventsWithMiddleDuplicate() {
            int orisId1 = 101;
            int orisId2 = 102;
            int orisId3 = 103;

            Event event1 = EventTestDataBuilder.anEvent().withName("Spring Sprint")
                    .withDate(LocalDate.of(2026, 6, 1)).build();
            Event event3 = EventTestDataBuilder.anEvent().withName("Autumn Race")
                    .withDate(LocalDate.of(2026, 9, 10)).build();

            stubSyncSuccess(orisId1, event1);
            stubSyncError(orisId2, "could not execute statement; unique constraint [uq_event_oris_id] violation");
            stubSyncSuccess(orisId3, event3);

            BulkImportResult result = service.importEventsFromOris(List.of(orisId1, orisId2, orisId3));

            assertThat(result.totalProcessed()).isEqualTo(3);
            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.failureCount()).isEqualTo(1);
            assertThat(result.results()).hasSize(3);

            BulkImportResult.EventImportEntry first = result.results().get(0);
            assertThat(first.status()).isEqualTo(BulkImportResult.ImportStatus.IMPORTED);
            assertThat(first.name()).isEqualTo("Spring Sprint");
            assertThat(first.date()).isEqualTo(LocalDate.of(2026, 6, 1));

            BulkImportResult.EventImportEntry middle = result.results().get(1);
            assertThat(middle.status()).isEqualTo(BulkImportResult.ImportStatus.FAILED);
            assertThat(middle.orisId()).isEqualTo(orisId2);
            assertThat(middle.error()).contains("unique constraint");

            BulkImportResult.EventImportEntry last = result.results().get(2);
            assertThat(last.status()).isEqualTo(BulkImportResult.ImportStatus.IMPORTED);
            assertThat(last.name()).isEqualTo("Autumn Race");
            assertThat(last.date()).isEqualTo(LocalDate.of(2026, 9, 10));
        }

        @Test
        @DisplayName("should record IMPORTED status with name and date read from the event repository")
        void shouldRecordImportedStatusForSuccessfulEvents() {
            int orisId = 201;
            LocalDate eventDate = LocalDate.of(2026, 8, 20);
            Event imported = EventTestDataBuilder.anEvent().withName("Championship").withDate(eventDate).build();

            stubSyncSuccess(orisId, imported);

            BulkImportResult result = service.importEventsFromOris(List.of(orisId));

            assertThat(result.results()).hasSize(1);
            BulkImportResult.EventImportEntry entry = result.results().getFirst();
            assertThat(entry.orisId()).isEqualTo(orisId);
            assertThat(entry.name()).isEqualTo("Championship");
            assertThat(entry.date()).isEqualTo(eventDate);
            assertThat(entry.status()).isEqualTo(BulkImportResult.ImportStatus.IMPORTED);
            assertThat(entry.error()).isNull();
        }

        @Test
        @DisplayName("should record FAILED status with error message when the sync record is an ERROR")
        void shouldRecordFailedStatusForErrorRecord() {
            int orisId = 301;

            stubSyncError(orisId, "unique constraint violation");

            BulkImportResult result = service.importEventsFromOris(List.of(orisId));

            assertThat(result.failureCount()).isEqualTo(1);
            BulkImportResult.EventImportEntry entry = result.results().getFirst();
            assertThat(entry.orisId()).isEqualTo(orisId);
            assertThat(entry.name()).isNull();
            assertThat(entry.date()).isNull();
            assertThat(entry.status()).isEqualTo(BulkImportResult.ImportStatus.FAILED);
            assertThat(entry.error()).isNotBlank();
        }

        @Test
        @DisplayName("should import all when every id succeeds")
        void shouldImportAllWhenEverySucceeds() {
            Event e1 = EventTestDataBuilder.anEvent().withName("E1").withDate(LocalDate.of(2026, 7, 1)).build();
            Event e2 = EventTestDataBuilder.anEvent().withName("E2").withDate(LocalDate.of(2026, 7, 2)).build();

            stubSyncSuccess(401, e1);
            stubSyncSuccess(402, e2);

            BulkImportResult result = service.importEventsFromOris(List.of(401, 402));

            assertThat(result.totalProcessed()).isEqualTo(2);
            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.results()).allMatch(r -> r.status() == BulkImportResult.ImportStatus.IMPORTED);
        }

        @Test
        @DisplayName("should fail all when every id returns an ERROR record")
        void shouldFailAllWhenEveryFails() {
            stubSyncError(501, "boom 1");
            stubSyncError(502, "boom 2");

            BulkImportResult result = service.importEventsFromOris(List.of(501, 502));

            assertThat(result.totalProcessed()).isEqualTo(2);
            assertThat(result.successCount()).isEqualTo(0);
            assertThat(result.failureCount()).isEqualTo(2);
            assertThat(result.results()).allMatch(r -> r.status() == BulkImportResult.ImportStatus.FAILED);
            assertThat(result.results().get(0).error()).isEqualTo("boom 1");
            assertThat(result.results().get(1).error()).isEqualTo("boom 2");
        }

        @Test
        @DisplayName("should return totalProcessed=0 with empty results for empty input list")
        void shouldReturnEmptyResultForEmptyList() {
            BulkImportResult result = service.importEventsFromOris(List.of());

            assertThat(result.totalProcessed()).isEqualTo(0);
            assertThat(result.successCount()).isEqualTo(0);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.results()).isEmpty();
            verifyNoInteractions(dataSync);
        }
    }
}
