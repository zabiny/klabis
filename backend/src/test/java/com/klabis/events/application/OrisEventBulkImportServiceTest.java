package com.klabis.events.application;

import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventBulkImportService — importEventsFromOris()")
class OrisEventBulkImportServiceTest {

    @Mock
    private OrisEventImportPort orisEventImportPort;

    private OrisEventBulkImportService service;

    @BeforeEach
    void setUp() {
        service = new OrisEventBulkImportService(orisEventImportPort);
    }

    @Nested
    @DisplayName("importEventsFromOris()")
    class ImportEventsFromOris {

        @Test
        @DisplayName("should return totalProcessed=3, successCount=2, failureCount=1 when middle event is duplicate")
        void shouldImportThreeEventsWithMiddleDuplicate() {
            int orisId1 = 101;
            int orisId2 = 102;
            int orisId3 = 103;

            Event event1 = EventTestDataBuilder.anEvent().withName("Spring Sprint").withDate(LocalDate.of(2026, 6, 1)).build();
            Event event3 = EventTestDataBuilder.anEvent().withName("Autumn Race").withDate(LocalDate.of(2026, 9, 10)).build();

            when(orisEventImportPort.importEventFromOris(orisId1)).thenReturn(event1);
            when(orisEventImportPort.importEventFromOris(orisId2)).thenThrow(new DuplicateOrisImportException(orisId2));
            when(orisEventImportPort.importEventFromOris(orisId3)).thenReturn(event3);

            BulkImportResult result = service.importEventsFromOris(List.of(orisId1, orisId2, orisId3));

            assertThat(result.totalProcessed()).isEqualTo(3);
            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.failureCount()).isEqualTo(1);
            assertThat(result.results()).hasSize(3);
        }

        @Test
        @DisplayName("should record IMPORTED status with correct name and date for successful events")
        void shouldRecordImportedStatusForSuccessfulEvents() {
            int orisId = 201;
            LocalDate eventDate = LocalDate.of(2026, 8, 20);
            Event imported = EventTestDataBuilder.anEvent().withName("Championship").withDate(eventDate).build();

            when(orisEventImportPort.importEventFromOris(orisId)).thenReturn(imported);

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
        @DisplayName("should record FAILED status with error message for duplicate event")
        void shouldRecordFailedStatusForDuplicateEvent() {
            int orisId = 301;

            when(orisEventImportPort.importEventFromOris(orisId)).thenThrow(new DuplicateOrisImportException(orisId));

            BulkImportResult result = service.importEventsFromOris(List.of(orisId));

            assertThat(result.failureCount()).isEqualTo(1);
            BulkImportResult.EventImportEntry entry = result.results().getFirst();
            assertThat(entry.orisId()).isEqualTo(orisId);
            assertThat(entry.status()).isEqualTo(BulkImportResult.ImportStatus.FAILED);
            assertThat(entry.error()).isNotBlank();
        }

        @Test
        @DisplayName("should continue processing remaining events after one fails")
        void shouldContinueAfterFailure() {
            int failingOrisId = 401;
            int successOrisId = 402;

            Event successEvent = EventTestDataBuilder.anEvent().withName("New Event").withDate(LocalDate.of(2026, 7, 1)).build();

            when(orisEventImportPort.importEventFromOris(failingOrisId)).thenThrow(new DuplicateOrisImportException(failingOrisId));
            when(orisEventImportPort.importEventFromOris(successOrisId)).thenReturn(successEvent);

            BulkImportResult result = service.importEventsFromOris(List.of(failingOrisId, successOrisId));

            assertThat(result.totalProcessed()).isEqualTo(2);
            assertThat(result.successCount()).isEqualTo(1);
            assertThat(result.failureCount()).isEqualTo(1);

            assertThat(result.results().get(0).status()).isEqualTo(BulkImportResult.ImportStatus.FAILED);
            assertThat(result.results().get(1).status()).isEqualTo(BulkImportResult.ImportStatus.IMPORTED);
        }

        @Test
        @DisplayName("should return totalProcessed=0 with empty results for empty input list")
        void shouldReturnEmptyResultForEmptyList() {
            BulkImportResult result = service.importEventsFromOris(List.of());

            assertThat(result.totalProcessed()).isEqualTo(0);
            assertThat(result.successCount()).isEqualTo(0);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.results()).isEmpty();
        }
    }
}
