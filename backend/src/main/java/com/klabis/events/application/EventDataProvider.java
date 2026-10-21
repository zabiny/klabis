package com.klabis.events.application;

import com.klabis.events.EventData;
import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;
import java.util.List;

/**
 * Port for reading event data from other modules (calendar).
 */
@PrimaryPort
public interface EventDataProvider {

    /**
     * @throws IllegalArgumentException if event not found
     */
    EventData getEventData(EventId eventId);

    /**
     * Event data for the given ids; ids of nonexistent events are skipped.
     */
    List<EventData> getEventData(Collection<EventId> eventIds);
}
