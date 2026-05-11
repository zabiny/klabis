package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventStatus;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisBulkSyncService")
class OrisBulkSyncServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrisEventImportPort orisEventImportPort;

    private OrisBulkSyncService service;

    @BeforeEach
    void setUp() {
        service = new OrisBulkSyncService(eventRepository, orisEventImportPort);
    }

    @Nested
    @DisplayName("syncAllUpcoming()")
    class SyncAllUpcoming {

        @Test
        @DisplayName("should return successCount=3, failureCount=0 when all 3 matching events sync successfully")
        void shouldSyncAllSuccessfully() {
            Event event1 = EventTestDataBuilder.anEvent().withName("Race A").withOrisId(101).build();
            Event event2 = EventTestDataBuilder.anEvent().withName("Race B").withOrisId(102).build();
            Event event3 = EventTestDataBuilder.anEvent().withName("Race C").withOrisId(103).build();

            when(eventRepository.findAllUpcomingOrisEvents(any(LocalDate.class)))
                    .thenReturn(List.of(event1, event2, event3));
            doNothing().when(orisEventImportPort).syncEventFromOris(any(EventId.class));

            BulkSyncResult result = service.syncAllUpcoming();

            assertThat(result.totalProcessed()).isEqualTo(3);
            assertThat(result.successCount()).isEqualTo(3);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.results()).hasSize(3);
            assertThat(result.results()).allMatch(e -> e.status() == BulkSyncResult.SyncStatus.SYNCED);
        }

        @Test
        @DisplayName("should return successCount=2, failureCount=1 with error when one event throws")
        void shouldAccumulateFailuresAndContinue() {
            Event event1 = EventTestDataBuilder.anEvent().withName("Race A").withOrisId(101).build();
            Event event2 = EventTestDataBuilder.anEvent().withName("Race B").withOrisId(102).build();
            Event event3 = EventTestDataBuilder.anEvent().withName("Race C").withOrisId(103).build();

            when(eventRepository.findAllUpcomingOrisEvents(any(LocalDate.class)))
                    .thenReturn(List.of(event1, event2, event3));
            doNothing().when(orisEventImportPort).syncEventFromOris(event1.getId());
            doThrow(new RuntimeException("ORIS endpoint returned 404"))
                    .when(orisEventImportPort).syncEventFromOris(event2.getId());
            doNothing().when(orisEventImportPort).syncEventFromOris(event3.getId());

            BulkSyncResult result = service.syncAllUpcoming();

            assertThat(result.totalProcessed()).isEqualTo(3);
            assertThat(result.successCount()).isEqualTo(2);
            assertThat(result.failureCount()).isEqualTo(1);

            BulkSyncResult.EventSyncEntry failedEntry = result.results().stream()
                    .filter(e -> e.status() == BulkSyncResult.SyncStatus.FAILED)
                    .findFirst()
                    .orElseThrow();
            assertThat(failedEntry.eventId()).isEqualTo(event2.getId());
            assertThat(failedEntry.name()).isEqualTo("Race B");
            assertThat(failedEntry.error()).isEqualTo("ORIS endpoint returned 404");
        }

        @Test
        @DisplayName("should process no events and return totalProcessed=0 when repository returns empty list")
        void shouldReturnZeroCountsWhenNoMatchingEvents() {
            when(eventRepository.findAllUpcomingOrisEvents(any(LocalDate.class)))
                    .thenReturn(List.of());

            BulkSyncResult result = service.syncAllUpcoming();

            assertThat(result.totalProcessed()).isEqualTo(0);
            assertThat(result.successCount()).isEqualTo(0);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.results()).isEmpty();
            verifyNoInteractions(orisEventImportPort);
        }

        @Test
        @DisplayName("should pass today's date to repository when finding upcoming events")
        void shouldPassTodayToRepository() {
            when(eventRepository.findAllUpcomingOrisEvents(any(LocalDate.class)))
                    .thenReturn(List.of());

            service.syncAllUpcoming();

            verify(eventRepository).findAllUpcomingOrisEvents(LocalDate.now());
        }
    }
}
