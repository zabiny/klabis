package com.klabis.calendar.eventhandlers;

import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.persistence.CalendarRepository;
import com.klabis.events.EventPublishedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Event handler for EventPublishedEvent.
 *
 * <p>This handler manages calendar item creation for newly published events.
 * It processes the EventPublishedEvent and creates a corresponding CalendarItem
 * to ensure events are visible in the calendar.
 *
 * <p><b>Architecture:</b> This is a cross-module event handler:
 * <ul>
 *   <li>Listens to events from the events module</li>
 *   <li>Creates calendar items in the calendar module</li>
 *   <li>Implements event-driven synchronization between bounded contexts</li>
 * </ul>
 *
 * <p><b>Idempotency:</b> This handler is idempotent - if a calendar item
 * already exists for the event, the handler logs a warning and skips creation.
 * This prevents duplicates when events are replayed or retried.
 *
 * <p><b>Event Flow:</b>
 * <ol>
 *   <li>EventPublishedEvent is emitted when Event transitions to ACTIVE status</li>
 *   <li>This handler receives the event via Spring Modulith</li>
 *   <li>Handler queries Event aggregate to get event details</li>
 *   <li>Handler creates CalendarItem with formatted description</li>
 * </ol>
 */
@Component
@PrimaryAdapter
public class EventPublishedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventPublishedEventHandler.class);

    private final CalendarRepository calendarRepository;
    private final EventDataProvider eventDataProvider;

    public EventPublishedEventHandler(
            CalendarRepository calendarRepository,
            EventDataProvider eventDataProvider) {
        this.calendarRepository = calendarRepository;
        this.eventDataProvider = eventDataProvider;
    }

    /**
     * Handles EventPublishedEvent by creating a calendar item for the published event.
     *
     * <p>The {@link ApplicationModuleListener} annotation provides:
     * <ul>
     *   <li>Event externalization via Spring Modulith's outbox pattern</li>
     *   <li>Automatic retry on failures</li>
     *   <li>Separate transaction for event processing</li>
     * </ul>
     *
     * <p><b>Idempotency:</b> If a calendar item already exists for this event,
     * the handler logs a warning and skips creation to prevent duplicates.
     *
     * @param event the event published event containing event ID
     */
    @ApplicationModuleListener
    public void handle(EventPublishedEvent event) {
        log.info("Processing EventPublishedEvent for event: {}", event.getEventId());

        // Check if calendar item already exists (idempotency)
        if (calendarRepository.findByEventId(event.getEventId()).isPresent()) {
            log.warn("Calendar item already exists for event {}. Skipping creation (idempotent).",
                    event.getEventId());
            return;
        }

        try {
            // Fetch event data from events module
            EventData eventData = eventDataProvider.getEventData(event.getEventId());

            // Build description: location + " - " + organizer + [newline + websiteUrl if present]
            String description = buildDescription(eventData);

            // Create calendar item
            CalendarItem calendarItem = CalendarItem.reconstruct(
                    com.klabis.calendar.CalendarItemId.generate(),
                    eventData.name(),
                    description,
                    eventData.eventDate(),
                    eventData.eventDate(), // For events, start and end date are the same
                    event.getEventId(),
                    null // Audit metadata will be set by repository
            );

            calendarRepository.save(calendarItem);

            log.info("Calendar item created successfully for event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to create calendar item for event: {}", event.getEventId(), e);
            throw e;
        }
    }

    /**
     * Builds calendar item description from event data.
     * <p>
     * Format: location + " - " + organizer + [newline + websiteUrl if present]
     *
     * @param eventData event data
     * @return formatted description
     */
    private String buildDescription(EventData eventData) {
        String baseDescription = eventData.location() + " - " + eventData.organizer();

        if (eventData.websiteUrl() != null) {
            return baseDescription + "\n" + eventData.websiteUrl().value();
        }

        return baseDescription;
    }
}
