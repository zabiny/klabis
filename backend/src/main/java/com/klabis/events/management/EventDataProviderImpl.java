package com.klabis.events.management;

import com.klabis.events.EventData;
import com.klabis.events.EventDataProvider;
import com.klabis.events.EventId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.Events;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

/**
 * Implementation of EventDataProvider that queries the events module.
 * <p>
 * This adapter uses the Events public API to fetch event data needed by
 * calendar event handlers. It provides a clean separation between modules
 * while allowing event-driven synchronization.
 */
@Component
@SecondaryAdapter
class EventDataProviderImpl implements EventDataProvider {

    private final Events events;

    EventDataProviderImpl(Events events) {
        this.events = events;
    }

    @Override
    public EventData getEventData(EventId eventId) {
        Event event = events.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        return new EventData(
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl().value()
        );
    }
}
