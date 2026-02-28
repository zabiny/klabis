package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Secondary port for fetching event data from the events module.
 * <p>
 * This port decouples the calendar module from direct dependencies on the events
 * module repositories. The implementation will query the events module through
 * its public API.
 * <p>
 * This is used by event handlers to fetch event details when processing
 * EventPublishedEvent (which only contains event ID).
 */
@SecondaryPort
public interface EventDataProvider {

    /**
     * Fetches event data for the specified event ID.
     *
     * @param eventId the event ID
     * @return event data
     * @throws IllegalArgumentException if event not found
     */
    EventData getEventData(EventId eventId);
}
