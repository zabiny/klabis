package club.klabis.calendar;

import club.klabis.events.domain.Event;
import club.klabis.shared.config.Globals;

import java.time.ZonedDateTime;
import java.util.Optional;

public class EventCalendarItem extends CalendarItem {
    private Event.Id eventId;

    public static Optional<EventCalendarItem> registrationsDeadlineItem(Event event) {
        if (event.getRegistrationDeadline() != null) {
            return Optional.of(new EventCalendarItem(Id.newId(),
                    event.getRegistrationDeadline(),
                    event.getRegistrationDeadline(),
                    "%s - Uzaverka prihlasek".formatted(event.getName()),
                    event.getId()));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<EventCalendarItem> eventDayItem(Event event) {
        if (event.getDate() != null) {
            return Optional.of(new EventCalendarItem(Id.newId(),
                    Globals.toZonedDateTime(event.getDate()),
                    Globals.toZonedDateTime(event.getDate()),
                    event.getName(),
                    event.getId()));
        } else {
            return Optional.empty();
        }
    }

    private EventCalendarItem(Id id, ZonedDateTime start, ZonedDateTime end, String note, Event.Id eventId) {
        super(id, start, end, note);
        this.eventId = eventId;
    }

    public Event.Id getEventId() {
        return eventId;
    }
}
