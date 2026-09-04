package com.klabis.events.infrastructure.listeners;

import com.klabis.events.EventCancelledEvent;
import com.klabis.events.EventFinishedEvent;
import com.klabis.events.EventId;
import com.klabis.events.EventUpdatedEvent;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventsSyncListener")
class EventsSyncListenerTest {

    @Mock
    private SynchronizationPort synchronizationPort;

    private EventsSyncListener listener;

    private EventId eventId;
    private SyncTarget target;

    @BeforeEach
    void setUp() {
        listener = new EventsSyncListener(synchronizationPort);
        eventId = new EventId(UUID.randomUUID());
        target = new SyncTarget(SyncEntityType.EVENT, eventId.value().toString());
    }

    @Test
    @DisplayName("marks the record dirty on EventUpdatedEvent (task 8.2)")
    void marksDirtyOnEventUpdated() {
        EventUpdatedEvent event = new EventUpdatedEvent(
                UUID.randomUUID(), eventId, "Sprint", java.time.LocalDate.now(),
                "Location", "Organizer", null, java.util.List.of(), Instant.now());

        listener.handle(event);

        verify(synchronizationPort).markDirty(target);
    }

    @Test
    @DisplayName("retires the record on EventFinishedEvent (task 8.1)")
    void retiresOnEventFinished() {
        EventFinishedEvent event = new EventFinishedEvent(UUID.randomUUID(), eventId, Instant.now());
        SyncRecordId recordId = SyncRecordId.newId();
        SyncRecord record = SyncRecord.enroll(recordId, target, new ExternalReference(ExternalSystem.ORIS, "100"));
        when(synchronizationPort.findByTarget(target)).thenReturn(Optional.of(record));

        listener.handle(event);

        verify(synchronizationPort).retire(recordId);
    }

    @Test
    @DisplayName("retires the record on EventCancelledEvent (task 8.1)")
    void retiresOnEventCancelled() {
        EventCancelledEvent event = new EventCancelledEvent(UUID.randomUUID(), eventId, Instant.now());
        SyncRecordId recordId = SyncRecordId.newId();
        SyncRecord record = SyncRecord.enroll(recordId, target, new ExternalReference(ExternalSystem.ORIS, "100"));
        when(synchronizationPort.findByTarget(target)).thenReturn(Optional.of(record));

        listener.handle(event);

        verify(synchronizationPort).retire(recordId);
    }

    @Test
    @DisplayName("does nothing when the event is not enrolled")
    void doesNothingWhenNotEnrolled() {
        EventFinishedEvent event = new EventFinishedEvent(UUID.randomUUID(), eventId, Instant.now());
        when(synchronizationPort.findByTarget(target)).thenReturn(Optional.empty());

        listener.handle(event);

        verify(synchronizationPort).findByTarget(target);
        verifyNoMoreInteractions(synchronizationPort);
    }
}
