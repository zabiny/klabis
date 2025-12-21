package club.klabis.calendar.infrastructure;

import club.klabis.calendar.CalendarItem;
import club.klabis.calendar.CalendarRepository;
import club.klabis.calendar.EventCalendarItem;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.EventDateChangedEvent;
import club.klabis.events.domain.EventRegistrationsDeadlineChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static club.klabis.shared.config.Globals.toZonedDateTime;

@Component
class EventListeners {

    private final CalendarRepository calendarRepository;

    EventListeners(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    private void setCalendarScheduleFromEvent(CalendarItem item, LocalDate date) {
        item.reschedule(toZonedDateTime(date), toZonedDateTime(date));
    }

    @EventListener(EventRegistrationsDeadlineChangedEvent.class)
    public void onDeadlineChanged(EventRegistrationsDeadlineChangedEvent event) {
        Event aggregate = event.getAggregate();
        EventCalendarItem item = calendarRepository.findEventItem(aggregate.getId(),
                        EventCalendarItem.Type.EVENT_REGISTRATION_DEADLINE)
                .map(eventItem -> {
                    eventItem.reschedule(aggregate.getRegistrationDeadline(), aggregate.getRegistrationDeadline());
                    return eventItem;
                })
                .orElseGet(() -> EventCalendarItem.registrationsDeadlineItem(aggregate));

        calendarRepository.save(item);
    }

    @EventListener(EventDateChangedEvent.class)
    public void onDeadlineChanged(EventDateChangedEvent event) {
        Event aggregate = event.getAggregate();
        EventCalendarItem item = calendarRepository.findEventItem(aggregate.getId(),
                        EventCalendarItem.Type.EVENT_DATE)
                .map(eventItem -> {
                    eventItem.reschedule(toZonedDateTime(aggregate.getDate()), toZonedDateTime(aggregate.getDate()));
                    return eventItem;
                })
                .orElseGet(() -> EventCalendarItem.eventDayItem(aggregate));
        calendarRepository.save(item);
    }

}
