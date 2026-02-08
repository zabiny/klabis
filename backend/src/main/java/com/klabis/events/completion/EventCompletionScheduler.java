package com.klabis.events.completion;

import com.klabis.events.Event;
import com.klabis.events.persistence.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job for automatically completing events after their event date has passed.
 * <p>
 * This scheduler runs daily at 2:00 AM and transitions ACTIVE events with past dates
 * to FINISHED status. DRAFT events are not affected.
 * <p>
 * Business rule: Only ACTIVE events with past dates should be completed.
 * DRAFT events remain DRAFT even if their date has passed.
 */
@Component
class EventCompletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventCompletionScheduler.class);

    private final EventRepository eventRepository;

    EventCompletionScheduler(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Scheduled method that runs daily at 2:00 AM to complete expired events.
     * Uses cron expression: "0 0 2 * * *" (second minute hour day month day-of-week)
     */
    @Scheduled(cron = "0 0 2 * * *")
    void completeExpiredEvents() {
        completeExpiredEvents(LocalDate.now());
    }

    /**
     * Complete expired events for a specific date.
     * <p>
     * This method is package-private to allow testing without triggering the scheduler.
     * Finds all ACTIVE events with eventDate before the specified date and transitions
     * them to FINISHED status.
     * <p>
     * Error handling:
     * - If repository query fails, the error is logged and scheduler exits gracefully
     * - If saving an individual event fails, the error is logged and processing continues
     * with the remaining events
     *
     * @param date the current date to check against
     */
    void completeExpiredEvents(LocalDate date) {
        log.info("Starting event completion scheduler for date: {}", date);

        List<Event> expiredEvents;
        try {
            expiredEvents = eventRepository.findActiveEventsWithDateBefore(date);
        } catch (Exception e) {
            log.error("Failed to query expired events from repository", e);
            return;
        }

        if (expiredEvents.isEmpty()) {
            log.info("No events to complete");
            return;
        }

        log.info("Found {} event(s) to complete", expiredEvents.size());

        int successCount = 0;
        int failureCount = 0;

        for (Event event : expiredEvents) {
            try {
                event.finish();
                eventRepository.save(event);
                successCount++;
                log.debug("Completed event: {} (id: {})", event.getName(), event.getId());
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to complete event: {} (id: {})", event.getName(), event.getId(), e);
                // Continue processing other events even if one fails
            }
        }

        log.info("Event completion finished. Success: {}, Failures: {}", successCount, failureCount);
    }
}
