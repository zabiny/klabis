package club.klabis.calendar;

import club.klabis.events.domain.Event;
import club.klabis.shared.config.Globals;

import java.time.ZonedDateTime;

public class EventCalendarItem extends CalendarItem {
    private Event.Id eventId;
    private Type type;

    public enum Type {
        EVENT_REGISTRATION_DEADLINE, EVENT_DATE
    }

    // TODO: refactor, remove Event instance from here
    public static EventCalendarItem registrationsDeadlineItem(Event event) {
        return new EventCalendarItem(Id.newId(),
                event.getRegistrationDeadline(),
                event.getRegistrationDeadline(),
                "%s - Uzaverka prihlasek".formatted(event.getName()),
                event.getId(), Type.EVENT_REGISTRATION_DEADLINE);
    }

    // TODO: refactor, remove Event instance from here
    public static EventCalendarItem eventDayItem(Event event) {
        return new EventCalendarItem(Id.newId(),
                Globals.toZonedDateTime(event.getDate()),
                Globals.toZonedDateTime(event.getDate()),
                event.getName(),
                event.getId(), Type.EVENT_DATE);
    }

    protected EventCalendarItem() {
    }

    private EventCalendarItem(Id id, ZonedDateTime start, ZonedDateTime end, String note, Event.Id eventId, Type type) {
        super(id, start, end, note);
        this.eventId = eventId;
        this.type = type;
    }

    public Event.Id getEventId() {
        return eventId;
    }

    public Type getType() {
        return type;
    }
}
