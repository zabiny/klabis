package com.klabis.calendar.eventsintegration;

import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.time.LocalDate;

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
     * @param eventId      the event ID
     * @param name         updated event name
     * @param eventDate    updated event date
     * @param location     updated location
     * @param organizer    updated organizer
     * @param websiteUrl   updated website URL (optional)
     */
    void handleEventUpdated(
            EventId eventId,
            String name,
            LocalDate eventDate,
            String location,
            String organizer,
            String websiteUrl);

    /**
     * Deletes a calendar item when an event is cancelled.
     *
     * @param eventId the event ID
     */
    void handleEventCancelled(EventId eventId);
}
