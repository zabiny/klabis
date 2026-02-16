package com.klabis.calendar.eventhandlers;

import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.persistence.CalendarRepository;
import com.klabis.events.EventCancelledEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Event handler for EventCancelledEvent.
 *
 * <p>This handler manages calendar item deletion when events are cancelled.
 * It processes the EventCancelledEvent and removes the corresponding CalendarItem
 * to keep the calendar in sync with event lifecycle.
 *
 * <p><b>Architecture:</b> This is a cross-module event handler:
 * <ul>
 *   <li>Listens to events from the events module</li>
 *   <li>Deletes calendar items in the calendar module</li>
 *   <li>Implements event-driven synchronization between bounded contexts</li>
 * </ul>
 *
 * <p><b>Idempotency:</b> This handler is idempotent - if no calendar item
 * exists for the event, the handler logs a warning and skips the deletion.
 * This handles cases where the calendar item was already deleted or the
 * event was cancelled before being published.
 *
 * <p><b>Event Flow:</b>
 * <ol>
 *   <li>EventCancelledEvent is emitted when Event transitions to CANCELLED status</li>
 *   <li>This handler receives the event via Spring Modulith</li>
 *   <li>Handler finds the linked CalendarItem by eventId</li>
 *   <li>Handler deletes the CalendarItem</li>
 * </ol>
 */
@Component
@PrimaryAdapter
public class EventCancelledEventHandler {

    private static final Logger log = LoggerFactory.getLogger(EventCancelledEventHandler.class);

    private final CalendarRepository calendarRepository;

    public EventCancelledEventHandler(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    /**
     * Handles EventCancelledEvent by deleting the linked calendar item.
     *
     * <p>The {@link ApplicationModuleListener} annotation provides:
     * <ul>
     *   <li>Event externalization via Spring Modulith's outbox pattern</li>
     *   <li>Automatic retry on failures</li>
     *   <li>Separate transaction for event processing</li>
     * </ul>
     *
     * <p><b>Idempotency:</b> If no calendar item exists for this event,
     * the handler logs a warning and skips the deletion. This is safe because:
     * <ul>
     *   <li>Calendar items are only created when events are published</li>
     *   <li>Events may be cancelled while still in DRAFT status (before publishing)</li>
     *   <li>Calendar items may have been manually deleted by administrators</li>
     *   <li>Event cancellation may be replayed or retried</li>
     * </ul>
     *
     * @param event the event cancelled event containing event ID
     */
    @ApplicationModuleListener
    public void handle(EventCancelledEvent event) {
        log.info("Processing EventCancelledEvent for event: {}", event.getEventId());

        // Find the linked calendar item
        Optional<CalendarItem> calendarItemOpt = calendarRepository.findByEventId(event.getEventId());

        if (calendarItemOpt.isEmpty()) {
            log.warn("No calendar item found for event {}. Skipping deletion (idempotent).",
                    event.getEventId());
            return;
        }

        try {
            CalendarItem calendarItem = calendarItemOpt.get();

            // Delete the calendar item
            calendarRepository.delete(calendarItem);

            log.info("Calendar item deleted successfully for event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to delete calendar item for event: {}", event.getEventId(), e);
            throw e;
        }
    }
}
