package com.klabis.events.application;

import com.klabis.events.EventId;
import com.klabis.events.EventTestDataBuilder;
import com.klabis.events.domain.*;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.application.SyncRecordNeedsResolutionException;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ORIS field-mapping coverage (organizer fallback, deadlines, ranking, base entry fee,
 * category orisId) lives in {@link OrisEventDetailsMapperTest} — {@code
 * importEventFromOris} no longer builds the {@link Event} itself, that moved to {@code
 * OrisEventSyncAdapter.createLocal} via the synchronisation engine's {@code
 * pullAndEnroll} (design.md D2, "ORIS import path delegates to the engine").
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventImportService")
class OrisEventImportServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private SynchronizationPort synchronizationPort;

    private OrisEventImportService service;

    @BeforeEach
    void setUp() {
        service = new OrisEventImportService(eventRepository, synchronizationPort);
    }

    @Nested
    @DisplayName("importEventFromOris()")
    class ImportEventFromOrisMethod {

        @Test
        @DisplayName("should delegate to the synchronisation engine's pullAndEnroll and return the resulting event (task 9.2)")
        void shouldDelegateToPullAndEnroll() {
            int orisId = 9877;
            EventId eventId = EventId.generate();
            Event event = EventTestDataBuilder.anEventWithId(eventId).build();

            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, eventId.value().toString());
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target,
                    new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));
            when(synchronizationPort.pullAndEnroll(eq(SyncEntityType.EVENT),
                    eq(new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId))), any()))
                    .thenReturn(record);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            Event result = service.importEventFromOris(orisId);

            assertThat(result).isEqualTo(event);
            verify(synchronizationPort).pullAndEnroll(SyncEntityType.EVENT,
                    new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)), null);
        }

        @Test
        @DisplayName("should synchronise and return the existing event on a repeated import (design.md D5, D10)")
        void shouldReturnExistingEventOnRepeatedImport() {
            int orisId = 1111;
            EventId eventId = EventId.generate();
            Event existingEvent = EventTestDataBuilder.anEventWithId(eventId).withName("Already Imported").build();

            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, eventId.value().toString());
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target,
                    new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));
            when(synchronizationPort.pullAndEnroll(eq(SyncEntityType.EVENT), any(), any()))
                    .thenReturn(record);
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

            Event result = service.importEventFromOris(orisId);

            assertThat(result).isEqualTo(existingEvent);
            assertThat(result.getName()).isEqualTo("Already Imported");
        }

        @Test
        @DisplayName("should refuse with EventSyncNeedsResolutionException when the pairing awaits a decision (design.md D5, D10)")
        void shouldRefuseWhenPairingNeedsResolution() {
            int orisId = 2222;
            ExternalReference reference = new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId));
            when(synchronizationPort.pullAndEnroll(eq(SyncEntityType.EVENT), eq(reference), any()))
                    .thenThrow(new SyncRecordNeedsResolutionException(SyncRecordId.newId(), SyncStatus.CONFLICT));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(EventSyncNeedsResolutionException.class);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when the engine's pass finds no local entity after creation (defensive)")
        void shouldThrowEventNotFoundWhenEventMissingAfterPullAndEnroll() {
            int orisId = 9999;
            EventId eventId = EventId.generate();
            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, eventId.value().toString());
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target,
                    new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisId)));
            when(synchronizationPort.pullAndEnroll(eq(SyncEntityType.EVENT), any(), any()))
                    .thenReturn(record);
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("syncEventFromOris()")
    class SyncEventFromOrisMethod {

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowWhenEventNotFound() {
            EventId eventId = EventId.generate();
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event is not enrolled for synchronisation")
        void shouldThrowWhenEventNotEnrolled() {
            EventId eventId = EventId.generate();
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Race").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Forest").organizer("OOB").build());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(synchronizationPort.findByTarget(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should delegate to the synchronisation engine when the record is enrolled and not stuck (task 8.3)")
        void shouldDelegateToSynchronizationEngine() {
            EventId eventId = EventId.generate();
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Race").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Forest").organizer("OOB").build());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, eventId.value().toString());
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), target,
                    new ExternalReference(ExternalSystem.ORIS, "9876"));
            when(synchronizationPort.findByTarget(target)).thenReturn(Optional.of(record));

            service.syncEventFromOris(eventId);

            verify(synchronizationPort).synchronizeNow(record.getId(), null);
        }

        @Test
        @DisplayName("should refuse with EventSyncNeedsResolutionException when the record is in conflict")
        void shouldRefuseWhenRecordInConflict() {
            EventId eventId = EventId.generate();
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .name("Race").eventDate(LocalDate.of(2026, 8, 1))
                    .location("Forest").organizer("OOB").build());
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            SyncTarget target = new SyncTarget(SyncEntityType.EVENT, eventId.value().toString());
            SyncRecord record = Mockito.mock(SyncRecord.class);
            when(record.getStatus()).thenReturn(SyncStatus.CONFLICT);
            when(synchronizationPort.findByTarget(target)).thenReturn(Optional.of(record));

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(EventSyncNeedsResolutionException.class);
        }
    }
}
