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
        @DisplayName("rethrows the exact DuplicateOrisImportException carried by the ERROR record")
        void shouldRethrowDuplicate() {
            int orisId = 1111;
            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            DuplicateOrisImportException failure = new DuplicateOrisImportException(orisId);
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(null, externalId, failure));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isSameAs(failure);
        }

        @Test
        @DisplayName("rethrows the exact EventNotFoundException carried by the ERROR record")
        void shouldRethrowNotFound() {
            int orisId = 9999;
            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            EventNotFoundException failure = new EventNotFoundException(orisId);
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(null, externalId, failure));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isSameAs(failure);
        }

        @Test
        @DisplayName("rethrows the BusinessRuleViolationException carried by the ERROR record")
        void shouldRethrowBusinessRuleViolation() {
            int orisId = 1003;
            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            BusinessRuleViolationException failure = new BusinessRuleViolationException(
                    "ORIS event 1003 has invalid registration deadlines") {
            };
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(null, externalId, failure));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isSameAs(failure);
        }

        @Test
        @DisplayName("rethrows an engine-internal IllegalStateException unchanged")
        void shouldRethrowEngineInternalError() {
            int orisId = 4242;
            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            IllegalStateException failure = new IllegalStateException("No sync record found for " + externalId);
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(null, externalId, failure));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isSameAs(failure);
        }

        @Test
        @DisplayName("wraps a checked exception carried by the ERROR record in IllegalStateException")
        void shouldWrapCheckedException() {
            int orisId = 5000;
            SyncId externalId = SyncId.externalId(SyncType.EVENT, Integer.toString(orisId));
            Exception checked = new Exception("checked failure");
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(null, externalId, checked));

            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCause(checked);
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
        @DisplayName("rethrows the exact exception carried by an ERROR record")
        void shouldRethrowErrorRecord() {
            EventId eventId = EventId.generate();
            Event event = EventTestDataBuilder.anEventWithId(eventId).withOrisId(77).build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            BusinessRuleViolationException failure = new BusinessRuleViolationException(
                    "ORIS event 77 has invalid registration deadlines") {
            };
            when(dataSync.sync(any(SyncId.class), any(DataSync.Direction.class)))
                    .thenReturn(SyncRecord.failure(
                            SyncId.localId(SyncType.EVENT, eventId.value().toString()), null, failure));

            assertThatThrownBy(() -> service.syncEventFromOris(eventId))
                    .isSameAs(failure);
        }
    }
}
