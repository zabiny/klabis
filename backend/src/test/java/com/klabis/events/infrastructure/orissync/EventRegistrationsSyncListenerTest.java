package com.klabis.events.infrastructure.orissync;

import com.klabis.events.EventCancelledEvent;
import com.klabis.events.EventFinishedEvent;
import com.klabis.events.EventId;
import com.klabis.events.MemberRegisteredForEventEvent;
import com.klabis.events.MemberUnregisteredFromEventEvent;
import com.klabis.events.RegistrationEditedEvent;
import com.klabis.events.UpdateOrigin;
import com.klabis.members.MemberId;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventRegistrationsSyncListener")
class EventRegistrationsSyncListenerTest {

    @Mock
    private SynchronizationPort synchronizationPort;

    private EventRegistrationsSyncListener listener;

    private EventId eventId;
    private MemberId memberId;
    private SyncTarget participation;

    @BeforeEach
    void setUp() {
        listener = new EventRegistrationsSyncListener(synchronizationPort);
        eventId = new EventId(UUID.randomUUID());
        memberId = new MemberId(UUID.randomUUID());
        participation = new SyncTarget(SyncEntityType.EVENT_REGISTRATION, eventId.value() + ":" + memberId.value());
    }

    private MemberRegisteredForEventEvent registered(UpdateOrigin origin) {
        return new MemberRegisteredForEventEvent(UUID.randomUUID(), eventId, memberId, Instant.now(), origin);
    }

    private RegistrationEditedEvent edited(UpdateOrigin origin) {
        return new RegistrationEditedEvent(UUID.randomUUID(), eventId, memberId, null, null, null, null,
                Instant.now(), origin);
    }

    private MemberUnregisteredFromEventEvent unregistered(UpdateOrigin origin) {
        return new MemberUnregisteredFromEventEvent(UUID.randomUUID(), eventId, memberId, Instant.now(), origin);
    }

    private SyncRecord recordOf(EventId event, MemberId member) {
        return SyncRecord.enroll(SyncRecordId.newId(),
                new SyncTarget(SyncEntityType.EVENT_REGISTRATION, event.value() + ":" + member.value()),
                new ExternalReference(ExternalSystem.ORIS, "1:2"));
    }

    @Test
    @DisplayName("marks the participation dirty on a MANUAL registration, edit and unregistration")
    void marksDirtyOnManualOrigin() {
        listener.handle(registered(UpdateOrigin.MANUAL));
        listener.handle(edited(UpdateOrigin.MANUAL));
        listener.handle(unregistered(UpdateOrigin.MANUAL));

        verify(synchronizationPort, org.mockito.Mockito.times(3)).markDirty(participation);
    }

    @Test
    @DisplayName("ignores registration events written by the synchronisation itself")
    void ignoresSynchronisationOrigin() {
        listener.handle(registered(UpdateOrigin.SYNCHRONISATION));
        listener.handle(edited(UpdateOrigin.SYNCHRONISATION));
        listener.handle(unregistered(UpdateOrigin.SYNCHRONISATION));

        verifyNoInteractions(synchronizationPort);
    }

    @Test
    @DisplayName("retires every participation record of the finished event, including a member who had unregistered")
    void retiresAllRecordsOfFinishedEvent() {
        SyncRecord mine = recordOf(eventId, memberId);
        SyncRecord unregisteredMember = recordOf(eventId, new MemberId(UUID.randomUUID()));
        SyncRecord otherEvent = recordOf(new EventId(UUID.randomUUID()), memberId);
        when(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT_REGISTRATION))
                .thenReturn(List.of(mine, unregisteredMember, otherEvent));

        listener.handle(new EventFinishedEvent(UUID.randomUUID(), eventId, Instant.now()));

        verify(synchronizationPort).findActiveByEntityType(SyncEntityType.EVENT_REGISTRATION);
        verify(synchronizationPort).retire(mine.getId());
        verify(synchronizationPort).retire(unregisteredMember.getId());
        verifyNoMoreInteractions(synchronizationPort);
    }

    @Test
    @DisplayName("retires the participation records of a cancelled event")
    void retiresOnCancelled() {
        SyncRecord mine = recordOf(eventId, memberId);
        when(synchronizationPort.findActiveByEntityType(SyncEntityType.EVENT_REGISTRATION))
                .thenReturn(List.of(mine));

        listener.handle(new EventCancelledEvent(UUID.randomUUID(), eventId, Instant.now()));

        verify(synchronizationPort).retire(mine.getId());
    }
}
