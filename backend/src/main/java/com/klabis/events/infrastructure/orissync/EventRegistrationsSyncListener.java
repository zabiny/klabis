package com.klabis.events.infrastructure.orissync;

import com.klabis.common.OrisIntegrationComponent;
import com.klabis.events.EventCancelledEvent;
import com.klabis.events.EventFinishedEvent;
import com.klabis.events.EventId;
import com.klabis.events.MemberRegisteredForEventEvent;
import com.klabis.events.MemberUnregisteredFromEventEvent;
import com.klabis.events.RegistrationEditedEvent;
import com.klabis.events.UpdateOrigin;
import com.klabis.events.domain.RegistrationSyncId;
import com.klabis.members.MemberId;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Keeps the participation sync records in step with Klabis-side changes: a {@code MANUAL} registration, edit or
 * unregistration wakes the record up, a {@code SYNCHRONISATION}-origin one is the engine's own write and is ignored.
 * When the event ends, every participation record of it is retired.
 */
@OrisIntegrationComponent
@PrimaryAdapter
class EventRegistrationsSyncListener {

    private final SynchronizationPort synchronizationPort;
    private final SyncPairingLookup pairingLookup;

    EventRegistrationsSyncListener(SynchronizationPort synchronizationPort, SyncPairingLookup pairingLookup) {
        this.synchronizationPort = synchronizationPort;
        this.pairingLookup = pairingLookup;
    }

    @ApplicationModuleListener
    void handle(MemberRegisteredForEventEvent event) {
        markDirtyWhenManual(event.origin(), event.eventId(), event.memberId());
    }

    @ApplicationModuleListener
    void handle(RegistrationEditedEvent event) {
        markDirtyWhenManual(event.origin(), event.eventId(), event.memberId());
    }

    @ApplicationModuleListener
    void handle(MemberUnregisteredFromEventEvent event) {
        markDirtyWhenManual(event.origin(), event.eventId(), event.memberId());
    }

    @ApplicationModuleListener
    void handle(EventFinishedEvent event) {
        retireAllOf(event.eventId());
    }

    @ApplicationModuleListener
    void handle(EventCancelledEvent event) {
        retireAllOf(event.eventId());
    }

    private void markDirtyWhenManual(UpdateOrigin origin, EventId eventId, MemberId memberId) {
        if (origin == UpdateOrigin.SYNCHRONISATION) {
            return;
        }
        synchronizationPort.markDirty(new SyncTarget(SyncEntityType.EVENT_REGISTRATION,
                new RegistrationSyncId(eventId, memberId).format()));
    }

    // Looked up by the id prefix, not by the event's current registrations: a member who has unregistered still
    // has a record.
    private void retireAllOf(EventId eventId) {
        pairingLookup.findActiveByEntityTypeAndEntityIdPrefix(
                        SyncEntityType.EVENT_REGISTRATION, RegistrationSyncId.prefixOf(eventId)).stream()
                .map(SyncRecord::getId)
                .forEach(synchronizationPort::retire);
    }
}
