package com.klabis.calendar.infrastructure.listeners;
import com.klabis.calendar.application.CalendarEventSyncPort;
import com.klabis.events.EventCancelledEvent;
import com.klabis.events.EventPublishedEvent;
import com.klabis.events.EventUpdatedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Primary adapter (event listener) for event-related domain events.
 * <p>
 * This adapter receives domain events from the events module and delegates
 * business logic to the application service layer via Primary Port.
 * <p>
 * <b>Architecture:</b> Thin adapter layer - no business logic here.
 * All business logic is in the service implementing {@link CalendarEventSyncPort}.
 */
@Component
@PrimaryAdapter
class EventsEventListener {

    private static final Logger log = LoggerFactory.getLogger(EventsEventListener.class);

    private final CalendarEventSyncPort calendarEventSyncPort;

    public EventsEventListener(CalendarEventSyncPort calendarEventSyncPort) {
        this.calendarEventSyncPort = calendarEventSyncPort;
    }

    /**
     * Handles EventPublishedEvent by delegating to application service.
     *
     * @param event the event published event containing event ID
     */
    @ApplicationModuleListener
    public void handle(EventPublishedEvent event) {
        log.debug("Received EventPublishedEvent for event: {}", event.getEventId());
        calendarEventSyncPort.handleEventPublished(event.getEventId());
    }

    /**
     * Handles EventUpdatedEvent by delegating to application service.
     *
     * @param event the event updated event containing new event data
     */
    @ApplicationModuleListener
    public void handle(EventUpdatedEvent event) {
        log.debug("Received EventUpdatedEvent for event: {}", event.eventId());
        calendarEventSyncPort.handleEventUpdated(
                event.eventId(),
                event.name(),
                event.eventDate(),
                event.location(),
                event.organizer(),
                event.websiteUrl() != null ? event.websiteUrl().value() : null
        );
    }

    /**
     * Handles EventCancelledEvent by delegating to application service.
     *
     * @param event the event cancelled event containing event ID
     */
    @ApplicationModuleListener
    public void handle(EventCancelledEvent event) {
        log.debug("Received EventCancelledEvent for event: {}", event.getEventId());
        calendarEventSyncPort.handleEventCancelled(event.getEventId());
    }
}
