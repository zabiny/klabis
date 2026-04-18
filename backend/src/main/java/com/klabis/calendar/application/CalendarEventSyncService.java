package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.events.EventData;
import com.klabis.events.EventDataProvider;
import com.klabis.events.EventId;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Transactional
    public void handleEventPublished(EventId eventId) {
        log.info("Reconciling calendar items for published event: {}", eventId);
        reconcile(eventId);
        log.info("Calendar items reconciled for published event: {}", eventId);
    }

    @Transactional
    public void handleEventUpdated(EventId eventId) {
        log.info("Reconciling calendar items for updated event: {}", eventId);
        reconcile(eventId);
        log.info("Calendar items reconciled for updated event: {}", eventId);
    }

    @Transactional
    public void handleEventCancelled(EventId eventId) {
        log.info("Deleting all calendar items for cancelled event: {}", eventId);

        List<EventCalendarItem> items = findEventCalendarItems(eventId);
        items.forEach(calendarRepository::delete);

        log.info("Deleted {} calendar item(s) for cancelled event: {}", items.size(), eventId);
    }

    private void reconcile(EventId eventId) {
        EventData event = eventDataProvider.getEventData(eventId);

        Map<CalendarItemKind, EventCalendarItem> existingByKind = findEventCalendarItems(eventId).stream()
                .collect(Collectors.toMap(EventCalendarItem::getKind, item -> item));

        Set<CalendarItemKind> expectedKinds = EnumSet.of(CalendarItemKind.EVENT_DATE);
        if (event.registrationDeadline() != null) {
            expectedKinds.add(CalendarItemKind.EVENT_REGISTRATION_DATE);
        }

        for (CalendarItemKind kind : expectedKinds) {
            EventCalendarItem existing = existingByKind.remove(kind);
            if (existing != null) {
                existing.synchronizeFromEvent(event);
                calendarRepository.save(existing);
            } else {
                calendarRepository.save(createItem(kind, event, eventId));
            }
        }

        existingByKind.values().forEach(calendarRepository::delete);
    }

    private EventCalendarItem createItem(CalendarItemKind kind, EventData event, EventId eventId) {
        return switch (kind) {
            case EVENT_DATE -> EventCalendarItem.createForEventDate(
                    new EventCalendarItem.CreateCalendarItemForEvent(
                            event.name(),
                            event.location(),
                            event.organizer(),
                            event.websiteUrl(),
                            event.eventDate(),
                            eventId));
            case EVENT_REGISTRATION_DATE -> EventCalendarItem.createForRegistrationDeadline(
                    event.name(), eventId, event.registrationDeadline());
            default -> throw new IllegalArgumentException("Unexpected event calendar item kind: " + kind);
        };
    }

    private List<EventCalendarItem> findEventCalendarItems(EventId eventId) {
        return calendarRepository.findByEventId(eventId).stream()
                .filter(EventCalendarItem.class::isInstance)
                .map(EventCalendarItem.class::cast)
                .toList();
    }
}
