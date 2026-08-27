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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisBulkSyncService")
class OrisBulkSyncServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private DataSync dataSync;

    private OrisBulkSyncService service;

    @BeforeEach
    void setUp() {
        service = new OrisBulkSyncService(eventRepository, dataSync);
    }

    private static SyncId localIdOf(Event event) {
        return SyncId.localId(SyncType.EVENT, event.getId().value().toString());
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
            when(dataSync.sync(any(SyncId.class), eq(DataSync.Direction.PULL)))
                    .thenAnswer(inv -> SyncRecord.success(inv.getArgument(0),
                            SyncId.externalId(SyncType.EVENT, "0")));

            BulkSyncResult result = service.syncAllUpcoming();

            assertThat(result.totalProcessed()).isEqualTo(3);
            assertThat(result.successCount()).isEqualTo(3);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.results()).hasSize(3);
            assertThat(result.results()).allMatch(e -> e.status() == BulkSyncResult.SyncStatus.SYNCED);
        }

        @Test
        @DisplayName("should return successCount=2, failureCount=1 with error when one event returns an ERROR record")
        void shouldAccumulateFailuresAndContinue() {
            Event event1 = EventTestDataBuilder.anEvent().withName("Race A").withOrisId(101).build();
            Event event2 = EventTestDataBuilder.anEvent().withName("Race B").withOrisId(102).build();
            Event event3 = EventTestDataBuilder.anEvent().withName("Race C").withOrisId(103).build();

            when(eventRepository.findAllUpcomingOrisEvents(any(LocalDate.class)))
                    .thenReturn(List.of(event1, event2, event3));
            when(dataSync.sync(eq(localIdOf(event1)), eq(DataSync.Direction.PULL)))
                    .thenReturn(SyncRecord.success(localIdOf(event1), SyncId.externalId(SyncType.EVENT, "101")));
            when(dataSync.sync(eq(localIdOf(event2)), eq(DataSync.Direction.PULL)))
                    .thenReturn(SyncRecord.failure(localIdOf(event2), null, new RuntimeException("boom")));
            when(dataSync.sync(eq(localIdOf(event3)), eq(DataSync.Direction.PULL)))
                    .thenReturn(SyncRecord.success(localIdOf(event3), SyncId.externalId(SyncType.EVENT, "103")));

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
            assertThat(failedEntry.error()).isEqualTo("boom");

            assertThat(result.results()).filteredOn(e -> e.status() == BulkSyncResult.SyncStatus.SYNCED)
                    .hasSize(2);
        }

        @Test
        @DisplayName("should call DataSync once per event returned by findAllUpcomingOrisEvents, each with its local SyncId")
        void shouldIterateUpcomingEventsWithLocalSyncId() {
            Event event1 = EventTestDataBuilder.anEvent().withOrisId(101).build();
            Event event2 = EventTestDataBuilder.anEvent().withOrisId(102).build();

            when(eventRepository.findAllUpcomingOrisEvents(any(LocalDate.class)))
                    .thenReturn(List.of(event1, event2));
            when(dataSync.sync(any(SyncId.class), eq(DataSync.Direction.PULL)))
                    .thenAnswer(inv -> SyncRecord.success(inv.getArgument(0),
                            SyncId.externalId(SyncType.EVENT, "0")));

            service.syncAllUpcoming();

            ArgumentCaptor<SyncId> captor = ArgumentCaptor.forClass(SyncId.class);
            verify(dataSync, times(2)).sync(captor.capture(), eq(DataSync.Direction.PULL));
            assertThat(captor.getAllValues())
                    .containsExactly(localIdOf(event1), localIdOf(event2));
            assertThat(captor.getAllValues()).allMatch(SyncId::isLocalId);
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
            verifyNoInteractions(dataSync);
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
