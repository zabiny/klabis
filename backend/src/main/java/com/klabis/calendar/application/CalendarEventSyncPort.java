package com.klabis.calendar.application;

import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Primary port for synchronizing calendar items with event lifecycle.
 * <p>
 * This port defines the interface for event-driven calendar synchronization.
 * Primary adapters (event handlers) depend on this port to trigger
 * calendar updates when events change.
 */
@PrimaryPort
public interface CalendarEventSyncPort {

    /**
     * Creates a calendar item for a published event.
     *
     * @param eventId the event ID
     */
    void handleEventPublished(EventId eventId);

    /**
     * Updates a calendar item when an event is modified.
     *
     * @param eventId the event ID
     */
    void handleEventUpdated(EventId eventId);

    /**
     * Deletes a calendar item when an event is cancelled.
     *
     * @param eventId the event ID
     */
    void handleEventCancelled(EventId eventId);
}
