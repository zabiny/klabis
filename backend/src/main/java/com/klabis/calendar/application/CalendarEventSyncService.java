package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.infrastructure.jdbc.CalendarRepository;
import com.klabis.events.EventData;
import com.klabis.events.EventDataProvider;
import com.klabis.events.EventId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for synchronizing calendar items with event lifecycle.
 * <p>
 * This service handles business logic for event-driven calendar synchronization:
 * <ul>
 *   <li>Creating calendar items when events are published</li>
 *   <li>Updating calendar items when events are modified</li>
 *   <li>Deleting calendar items when events are cancelled</li>
 * </ul>
 * <p>
 * All operations are idempotent to handle event replays and retries.
 */
@Service
public class CalendarEventSyncService implements CalendarEventSyncPort {

    private static final Logger log = LoggerFactory.getLogger(CalendarEventSyncService.class);

    private final CalendarRepository calendarRepository;
    private final EventDataProvider eventDataProvider;

    public CalendarEventSyncService(
            CalendarRepository calendarRepository,
            EventDataProvider eventDataProvider) {
        this.calendarRepository = calendarRepository;
        this.eventDataProvider = eventDataProvider;
    }

    /**
     * Creates a calendar item for a published event.
     * <p>
     * Idempotent: if a calendar item already exists for this event, skips creation.
     *
     * @param eventId the event ID
     */
    @Transactional
    public void handleEventPublished(EventId eventId) {
        log.info("Creating calendar item for published event: {}", eventId);

        // Check if calendar item already exists (idempotency)
        if (calendarRepository.findByEventId(eventId).isPresent()) {
            log.warn("Calendar item already exists for event {}. Skipping creation (idempotent).", eventId);
            return;
        }

        // Fetch event data from events module
        EventData eventData = eventDataProvider.getEventData(eventId);

        // Build description: location + " - " + organizer + [newline + websiteUrl if present]
        String description = buildDescription(eventData);

        // Create event-linked calendar item using business factory method
        CalendarItem calendarItem = CalendarItem.createForEvent(
                eventData.name(),
                description,
                eventData.eventDate(),
                eventId
        );

        calendarRepository.save(calendarItem);

        log.info("Calendar item created successfully for event: {}", eventId);
    }

    /**
     * Updates a calendar item when an event is modified.
     * <p>
     * Idempotent: if no calendar item exists for this event, logs warning and skips.
     *
     * @param eventId    the event ID
     * @param name       updated event name
     * @param eventDate  updated event date
     * @param location   updated location
     * @param organizer  updated organizer
     * @param websiteUrl updated website URL (optional)
     */
    @Transactional
    public void handleEventUpdated(
            EventId eventId,
            String name,
            java.time.LocalDate eventDate,
            String location,
            String organizer,
            String websiteUrl) {

        log.info("Updating calendar item for event: {}", eventId);

        var calendarItemOpt = calendarRepository.findByEventId(eventId);

        if (calendarItemOpt.isEmpty()) {
            log.warn(
                    "Calendar item not found for event {}. Cannot update. Event may have been updated before being published.",
                    eventId);
            return;
        }

        CalendarItem calendarItem = calendarItemOpt.get();

        // Build updated description
        String description = buildDescription(location, organizer, websiteUrl);

        // Synchronize calendar item with updated event data using domain method
        calendarItem.synchronizeFromEvent(name, description, eventDate);

        calendarRepository.save(calendarItem);

        log.info("Calendar item updated successfully for event: {}", eventId);
    }

    /**
     * Deletes a calendar item when an event is cancelled.
     * <p>
     * Idempotent: if no calendar item exists for this event, logs warning and skips.
     *
     * @param eventId the event ID
     */
    @Transactional
    public void handleEventCancelled(EventId eventId) {
        log.info("Deleting calendar item for cancelled event: {}", eventId);

        var calendarItemOpt = calendarRepository.findByEventId(eventId);

        if (calendarItemOpt.isEmpty()) {
            log.warn(
                    "Calendar item not found for event {}. Cannot delete. Event may have been cancelled before being published.",
                    eventId);
            return;
        }

        CalendarItem calendarItem = calendarItemOpt.get();
        calendarRepository.delete(calendarItem);

        log.info("Calendar item deleted successfully for event: {}", eventId);
    }

    private String buildDescription(EventData eventData) {
        return buildDescription(eventData.location(), eventData.organizer(),
                eventData.websiteUrl() != null ? eventData.websiteUrl() : null);
    }

    private String buildDescription(String location, String organizer, String websiteUrl) {
        String baseDescription = location + " - " + organizer;

        if (websiteUrl != null) {
            return baseDescription + "\n" + websiteUrl;
        }

        return baseDescription;
    }
}
