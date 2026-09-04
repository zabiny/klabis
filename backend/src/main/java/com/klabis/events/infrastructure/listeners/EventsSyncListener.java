package com.klabis.events.infrastructure.listeners;

import com.klabis.events.EventCancelledEvent;
import com.klabis.events.EventFinishedEvent;
import com.klabis.events.EventId;
import com.klabis.events.EventUpdatedEvent;
import com.klabis.oris.OrisIntegrationComponent;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Self-listener on the {@code events} module's own domain events (design.md D9, D17,
 * task 8.1, 8.2): marks a record dirty on {@link EventUpdatedEvent}, and retires it
 * once the event reaches the end of its life ({@link EventFinishedEvent},
 * {@link EventCancelledEvent}).
 * <p>
 * A {@code @PrimaryAdapter} event listener is permitted to call a foreign module's
 * primary port ({@link SynchronizationPort}, in {@code sync.application}) — the same
 * pattern as {@code calendar.infrastructure.listeners.EventsEventListener}. Not every
 * event carries an enrolled record (a manually created event is never enrolled;
 * {@link SynchronizationPort#markDirty} and this listener's own {@code findByTarget}
 * lookup are both no-ops in that case), so no {@code orisId != null} filter is needed
 * here — the engine already knows which entities it manages.
 */
@OrisIntegrationComponent
@PrimaryAdapter
class EventsSyncListener {

    private final SynchronizationPort synchronizationPort;

    EventsSyncListener(SynchronizationPort synchronizationPort) {
        this.synchronizationPort = synchronizationPort;
    }

    @ApplicationModuleListener
    void handle(EventUpdatedEvent event) {
        synchronizationPort.markDirty(targetFor(event.eventId()));
    }

    @ApplicationModuleListener
    void handle(EventFinishedEvent event) {
        retire(event.eventId());
    }

    @ApplicationModuleListener
    void handle(EventCancelledEvent event) {
        retire(event.eventId());
    }

    private void retire(EventId eventId) {
        synchronizationPort.findByTarget(targetFor(eventId))
                .map(SyncRecord::getId)
                .ifPresent(synchronizationPort::retire);
    }

    private static SyncTarget targetFor(EventId eventId) {
        return new SyncTarget(SyncEntityType.EVENT, eventId.value().toString());
    }
}
