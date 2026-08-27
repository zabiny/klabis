package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.sync.DataSync;
import com.klabis.common.sync.SyncId;
import com.klabis.common.sync.SyncRecord;
import com.klabis.common.sync.SyncType;
import com.klabis.events.EventId;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrisEventImportService")
class OrisEventImportServiceTest {

    @Mock
    private DataSync dataSync;

    @Mock
    private EventRepository eventRepository;

    private OrisEventImportService service;

    @BeforeEach
    void setUp() {
        service = new OrisEventImportService(dataSync, eventRepository);
    }

    @Nested
    @DisplayName("importEventFromOris()")
    class ImportEventFromOrisMethod {

        @Test
        @DisplayName("delegates to DataSync with the external SyncId and returns the imported event")
        void shouldImportSuccessfully() {
            int orisId = 9876;
            EventId eventId = EventId.generate();
            Event imported = EventTestDataBuilder.anEventWithId(eventId).withOrisId(orisId).build();

            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            SyncId localId = SyncId.localId(SyncType.EVENT, eventId.value().toString());
            when(dataSync.sync(externalId, DataSync.Direction.PULL))
                    .thenReturn(SyncRecord.success(localId, externalId));
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(imported));

            Event result = service.importEventFromOris(orisId);

            assertThat(result).isSameAs(imported);
        }

        @Test
        @DisplayName("translates a unique-constraint failure to DuplicateOrisImportException")
        void shouldTranslateDuplicate() {
            int orisId = 1111;
            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(null, externalId,
                            "could not execute statement; SQL [n/a]; constraint [uq_event_oris_id]; unique constraint violation"));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(DuplicateOrisImportException.class);
        }

        @Test
        @DisplayName("translates a not-found failure to EventNotFoundException")
        void shouldTranslateNotFound() {
            int orisId = 9999;
            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(null, externalId,
                            "External item with ID SyncId[type=EVENT, party=EXTERNAL, idValue=9999] not found"));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("translates an invalid-deadlines failure to BusinessRuleViolationException")
        void shouldTranslateInvalidDeadlines() {
            int orisId = 1003;
            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(null, externalId,
                            "ORIS event 1003 has invalid registration deadlines: deadline1 and deadline3 set but deadline2 missing"));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("syncEventFromOris()")
    class SyncEventFromOrisMethod {

        @Test
        @DisplayName("delegates to DataSync with the local SyncId when the event exists and has an orisId")
        void shouldSyncSuccessfully() {
            EventId eventId = EventId.generate();
            Event event = EventTestDataBuilder.anEventWithId(eventId).withOrisId(42).build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            SyncId localId = SyncId.localId(SyncType.EVENT, eventId.value().toString());
            when(dataSync.sync(localId, DataSync.Direction.PULL))
                    .thenReturn(SyncRecord.success(localId, SyncId.externalId(SyncType.EVENT, "42")));

            assertThatCode(() -> service.syncEventFromOris(eventId)).doesNotThrowAnyException();

            ArgumentCaptor<SyncId> captor = ArgumentCaptor.forClass(SyncId.class);
            verify(dataSync).sync(captor.capture(), any(DataSync.Direction.class));
            assertThat(captor.getValue().isLocalId()).isTrue();
            assertThat(captor.getValue().idValue()).isEqualTo(eventId.value().toString());
        }

        @Test
        @DisplayName("throws EventNotFoundException and never calls DataSync when the event does not exist")
        void shouldThrowWhenEventNotFound() {
            EventId eventId = EventId.generate();
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(EventNotFoundException.class);
            verifyNoInteractions(dataSync);
        }

        @Test
        @DisplayName("throws BusinessRuleViolationException and never calls DataSync when the event has no orisId")
        void shouldThrowWhenEventHasNoOrisId() {
            EventId eventId = EventId.generate();
            Event event = EventTestDataBuilder.anEventWithId(eventId).build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(BusinessRuleViolationException.class);
            verify(dataSync, never()).sync(any(), any());
        }

        @Test
        @DisplayName("translates an ERROR record with a deadline failure cause to BusinessRuleViolationException")
        void shouldTranslateErrorRecord() {
            EventId eventId = EventId.generate();
            Event event = EventTestDataBuilder.anEventWithId(eventId).withOrisId(77).build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(
                            SyncId.localId(SyncType.EVENT, eventId.value().toString()), null,
                            "ORIS event 77 has invalid registration deadlines"));

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }
}
