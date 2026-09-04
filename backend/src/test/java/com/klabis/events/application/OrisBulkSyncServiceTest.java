package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventRepository;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisBulkSyncService")
class OrisBulkSyncServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SynchronizationPort synchronizationPort;

    private OrisBulkSyncService service;

    @BeforeEach
    void setUp() {
        service = new OrisBulkSyncService(eventRepository, synchronizationPort);
    }

    private static SyncRecord recordFor(EventId eventId, SyncStatus status) {
        SyncRecord record = SyncRecord.enroll(
                SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()),
                new ExternalReference(ExternalSystem.ORIS, "101"));
        return SyncRecord.reconstruct(
                record.getId(), record.getTarget(), record.getExternalReference(), status,
                record.getBaseline(), record.getLocal(), record.getExternal(), record.getExternalVersion(),
                record.getDirtySince(), record.getClaimedAt(), record.getAcknowledgement(),
                record.getNextAttemptDueAt(), record.getLastSuccessfulSyncAt(), record.getLastDirection(),
                record.getRetiredAt());
    }

    @Nested
    @DisplayName("syncAllUpcoming()")
    class SyncAllUpcoming {

        @Test
        @DisplayName("should return successCount=3, failureCount=0 when all 3 active records sync successfully")
        void shouldSyncAllSuccessfully() {
            Event event1 = EventTestDataBuilder.anEvent().withName("Race A").withOrisId(101).build();
            Event event2 = EventTestDataBuilder.anEvent().withName("Race B").withOrisId(102).build();
            Event event3 = EventTestDataBuilder.anEvent().withName("Race C").withOrisId(103).build();

            SyncRecord record1 = recordFor(event1.getId(), SyncStatus.NEW);
            SyncRecord record2 = recordFor(event2.getId(), SyncStatus.NEW);
            SyncRecord record3 = recordFor(event3.getId(), SyncStatus.NEW);

            when(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT))
                    .thenReturn(List.of(record1, record2, record3));
            when(eventRepository.findById(event1.getId())).thenReturn(Optional.of(event1));
            when(eventRepository.findById(event2.getId())).thenReturn(Optional.of(event2));
            when(eventRepository.findById(event3.getId())).thenReturn(Optional.of(event3));
            when(synchronizationPort.synchronizeNow(any(SyncRecordId.class), any()))
                    .thenReturn(record1);

            BulkSyncResult result = service.syncAllUpcoming();

            assertThat(result.totalProcessed()).isEqualTo(3);
            assertThat(result.successCount()).isEqualTo(3);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.awaitingDecisionCount()).isEqualTo(0);
            assertThat(result.stoppedByFailureCount()).isEqualTo(0);
            assertThat(result.results()).hasSize(3);
            assertThat(result.results()).allMatch(e -> e.status() == BulkSyncResult.SyncStatus.SYNCED);
        }

        @Test
        @DisplayName("should return successCount=2, failureCount=1 with error when one record throws")
        void shouldAccumulateFailuresAndContinue() {
            Event event1 = EventTestDataBuilder.anEvent().withName("Race A").withOrisId(101).build();
            Event event2 = EventTestDataBuilder.anEvent().withName("Race B").withOrisId(102).build();
            Event event3 = EventTestDataBuilder.anEvent().withName("Race C").withOrisId(103).build();

            SyncRecord record1 = recordFor(event1.getId(), SyncStatus.NEW);
            SyncRecord record2 = recordFor(event2.getId(), SyncStatus.NEW);
            SyncRecord record3 = recordFor(event3.getId(), SyncStatus.NEW);

            when(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT))
                    .thenReturn(List.of(record1, record2, record3));
            when(eventRepository.findById(event1.getId())).thenReturn(Optional.of(event1));
            when(eventRepository.findById(event2.getId())).thenReturn(Optional.of(event2));
            when(eventRepository.findById(event3.getId())).thenReturn(Optional.of(event3));
            when(synchronizationPort.synchronizeNow(record1.getId(), null)).thenReturn(record1);
            when(synchronizationPort.synchronizeNow(record2.getId(), null))
                    .thenThrow(new RuntimeException("ORIS endpoint returned 404"));
            when(synchronizationPort.synchronizeNow(record3.getId(), null)).thenReturn(record3);

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
        @DisplayName("should process no records and return totalProcessed=0 when no active records")
        void shouldReturnZeroCountsWhenNoActiveRecords() {
            when(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT))
                    .thenReturn(List.of());

            BulkSyncResult result = service.syncAllUpcoming();

            assertThat(result.totalProcessed()).isEqualTo(0);
            assertThat(result.successCount()).isEqualTo(0);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.awaitingDecisionCount()).isEqualTo(0);
            assertThat(result.stoppedByFailureCount()).isEqualTo(0);
            assertThat(result.results()).isEmpty();
            verifyNoInteractions(eventRepository);
        }

        @Test
        @DisplayName("should count and list CONFLICT records under awaitingDecision without attempting them")
        void shouldSeparateConflictRecords() {
            Event event1 = EventTestDataBuilder.anEvent().withName("Race A").withOrisId(101).build();
            SyncRecord conflicted = recordFor(event1.getId(), SyncStatus.CONFLICT);

            when(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT))
                    .thenReturn(List.of(conflicted));
            when(eventRepository.findById(event1.getId())).thenReturn(Optional.of(event1));

            BulkSyncResult result = service.syncAllUpcoming();

            assertThat(result.totalProcessed()).isEqualTo(1);
            assertThat(result.successCount()).isEqualTo(0);
            assertThat(result.failureCount()).isEqualTo(0);
            assertThat(result.awaitingDecisionCount()).isEqualTo(1);
            assertThat(result.stoppedByFailureCount()).isEqualTo(0);
            assertThat(result.awaitingDecision()).hasSize(1);
            assertThat(result.awaitingDecision().getFirst().eventId()).isEqualTo(event1.getId());
            assertThat(result.results()).isEmpty();
            verify(synchronizationPort, never()).synchronizeNow(any(SyncRecordId.class), any());
        }

        @Test
        @DisplayName("should count and list FAILED records under stoppedByFailure without attempting them")
        void shouldSeparateFailedRecords() {
            Event event1 = EventTestDataBuilder.anEvent().withName("Race A").withOrisId(101).build();
            SyncRecord failed = recordFor(event1.getId(), SyncStatus.FAILED);

            when(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT))
                    .thenReturn(List.of(failed));
            when(eventRepository.findById(event1.getId())).thenReturn(Optional.of(event1));

            BulkSyncResult result = service.syncAllUpcoming();

            assertThat(result.totalProcessed()).isEqualTo(1);
            assertThat(result.stoppedByFailureCount()).isEqualTo(1);
            assertThat(result.stoppedByFailure()).hasSize(1);
            assertThat(result.stoppedByFailure().getFirst().eventId()).isEqualTo(event1.getId());
            verify(synchronizationPort, never()).synchronizeNow(any(SyncRecordId.class), any());
        }
    }
}
