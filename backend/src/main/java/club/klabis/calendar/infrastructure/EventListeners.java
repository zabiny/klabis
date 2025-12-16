package club.klabis.calendar.infrastructure;

import club.klabis.calendar.CalendarRepository;
import club.klabis.calendar.EventCalendarItem;
import club.klabis.events.domain.EventDateChangedEvent;
import club.klabis.events.domain.EventRegistrationsDeadlineChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class EventListeners {

    private final CalendarService calendarService;
    private final CalendarRepository calendarRepository;

    EventListeners(CalendarService calendarService, CalendarRepository calendarRepository) {
        this.calendarService = calendarService;
        this.calendarRepository = calendarRepository;
    }

    @EventListener(EventRegistrationsDeadlineChangedEvent.class)
    public void onDeadlineChanged(EventRegistrationsDeadlineChangedEvent event) {
        EventCalendarItem.registrationsDeadlineItem(event.getAggregate()).ifPresent(calendarRepository::save);
    }

    @EventListener(EventDateChangedEvent.class)
    public void onDeadlineChanged(EventDateChangedEvent event) {
        EventCalendarItem.eventDayItem(event.getAggregate()).ifPresent(calendarRepository::save);
    }

}
