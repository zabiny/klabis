package com.klabis.calendar.eventhandlers;

import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.persistence.CalendarRepository;
import com.klabis.events.EventUpdatedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Event handler for EventUpdatedEvent.
 *
 * <p>This handler manages calendar item updates when events are modified.
 * It processes the EventUpdatedEvent and updates the corresponding CalendarItem
 * to keep the calendar in sync with event data.
 *
 * <p><b>Architecture:</b> This is a cross-module event handler:
 * <ul>
 *   <li>Listens to events from the events module</li>
 *   <li>Updates calendar items in the calendar module</li>
 *   <li>Implements event-driven synchronization between bounded contexts</li>
 * </ul>
 *
 * <p><b>Idempotency:</b> This handler is idempotent - if no calendar item
 * exists for the event, the handler logs a warning and skips the update.
 * This handles cases where the calendar item was manually deleted or the
 * event was updated before being published.
 *
 * <p><b>Event Flow:</b>
 * <ol>
 *   <li>EventUpdatedEvent is emitted when Event is modified (DRAFT or ACTIVE status)</li>
 *   <li>This handler receives the event via Spring Modulith</li>
 *   <li>Handler finds the linked CalendarItem by eventId</li>
 *   <li>Handler updates CalendarItem with new event data</li>
 * </ol>
 *
 * <p><b>Note:</b> EventUpdatedEvent contains all event data, so no additional
 * queries to the events module are needed (unlike EventPublishedEvent).
 */
@Component
@PrimaryAdapter
public class EventUpdatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventUpdatedEventHandler.class);

    private final CalendarRepository calendarRepository;

    public EventUpdatedEventHandler(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    /**
     * Handles EventUpdatedEvent by updating the linked calendar item.
     *
     * <p>The {@link ApplicationModuleListener} annotation provides:
     * <ul>
     *   <li>Event externalization via Spring Modulith's outbox pattern</li>
     *   <li>Automatic retry on failures</li>
     *   <li>Separate transaction for event processing</li>
     * </ul>
     *
     * <p><b>Idempotency:</b> If no calendar item exists for this event,
     * the handler logs a warning and skips the update. This is safe because:
     * <ul>
     *   <li>Calendar items are created when events are published</li>
     *   <li>Events may be updated while still in DRAFT status (before publishing)</li>
     *   <li>Calendar items may be manually deleted by administrators</li>
     * </ul>
     *
     * @param event the event updated event containing new event data
     */
    @ApplicationModuleListener
    public void handle(EventUpdatedEvent event) {
        log.info("Processing EventUpdatedEvent for event: {}", event.eventId());

        // Find the linked calendar item
        Optional<CalendarItem> calendarItemOpt = calendarRepository.findByEventId(event.eventId());

        if (calendarItemOpt.isEmpty()) {
            log.warn("No calendar item found for event {}. Skipping update (idempotent).",
                    event.eventId());
            return;
        }

        try {
            CalendarItem calendarItem = calendarItemOpt.get();

            // Build description: location + " - " + organizer + [newline + websiteUrl if present]
            String description = buildDescription(event);

            // Update calendar item with new data
            // Note: We use reconstruct to bypass the business rule check that prevents
            // manual updates to event-linked items. This is an internal synchronization
            // operation, not a manual user update.
            CalendarItem updatedItem = CalendarItem.reconstruct(
                    calendarItem.getId(),
                    event.name(),
                    description,
                    event.eventDate(),
                    event.eventDate(), // For events, start and end date are the same
                    event.eventId(),
                    calendarItem.getAuditMetadata() // Preserve existing audit metadata
            );

            calendarRepository.save(updatedItem);

            log.info("Calendar item updated successfully for event: {}", event.eventId());
        } catch (Exception e) {
            log.error("Failed to update calendar item for event: {}", event.eventId(), e);
            throw e;
        }
    }

    /**
     * Builds calendar item description from event data.
     * <p>
     * Format: location + " - " + organizer + [newline + websiteUrl if present]
     *
     * @param event event updated event
     * @return formatted description
     */
    private String buildDescription(EventUpdatedEvent event) {
        String baseDescription = event.location() + " - " + event.organizer();

        if (event.websiteUrl() != null) {
            return baseDescription + "\n" + event.websiteUrl().value();
        }

        return baseDescription;
    }
}
