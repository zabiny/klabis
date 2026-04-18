package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.events.EventData;
import com.klabis.events.EventDataProvider;
import com.klabis.events.EventId;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

    private Stream<EventCalendarItem> findEventCalendarItems(EventId eventId) {
        return calendarRepository.findByEventId(eventId).stream()
                .filter(EventCalendarItem.class::isInstance)
                .map(EventCalendarItem.class::cast);
    }

    @Transactional
    public void handleEventPublished(EventId eventId) {
        log.info("Creating calendar item for published event: {}", eventId);

        boolean alreadyExists = findEventCalendarItems(eventId).findAny().isPresent();

        if (alreadyExists) {
            log.warn("Calendar item already exists for event {}. Skipping creation (idempotent).", eventId);
            return;
        }

        EventData eventData = eventDataProvider.getEventData(eventId);

        EventCalendarItem calendarItem = EventCalendarItem.createForEvent(
                new EventCalendarItem.CreateCalendarItemForEvent(
                        eventData.name(),
                        eventData.location(),
                        eventData.organizer(),
                        eventData.websiteUrl(),
                        eventData.eventDate(),
                        eventId));

        calendarRepository.save(calendarItem);

        log.info("Calendar item created successfully for event: {}", eventId);
    }

    @Transactional
    public void handleEventUpdated(EventId eventId) {
        log.info("Updating calendar item for event: {}", eventId);

        Optional<EventCalendarItem> calendarItemOpt = findEventCalendarItems(eventId).findFirst();

        if (calendarItemOpt.isEmpty()) {
            log.warn(
                    "Calendar item not found for event {}. Cannot update. Event may have been updated before being published.",
                    eventId);
            return;
        }

        EventCalendarItem calendarItem = calendarItemOpt.get();

        EventData eventData = eventDataProvider.getEventData(eventId);

        calendarItem.synchronizeFromEvent(new EventCalendarItem.SynchronizeFromEvent(
                eventData.name(),
                eventData.location(),
                eventData.organizer(),
                eventData.websiteUrl(),
                eventData.eventDate()));

        calendarRepository.save(calendarItem);

        log.info("Calendar item updated successfully for event: {}", eventId);
    }

    @Transactional
    public void handleEventCancelled(EventId eventId) {
        log.info("Deleting calendar item for cancelled event: {}", eventId);

        List<EventCalendarItem> items = findEventCalendarItems(eventId).toList();

        if (items.isEmpty()) {
            log.warn(
                    "Calendar item not found for event {}. Cannot delete. Event may have been cancelled before being published.",
                    eventId);
            return;
        }

        items.forEach(calendarRepository::delete);

        log.info("Calendar item deleted successfully for event: {}", eventId);
    }
}
