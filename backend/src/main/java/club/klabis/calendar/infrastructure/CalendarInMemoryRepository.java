package club.klabis.calendar.infrastructure;

import club.klabis.calendar.Calendar;
import club.klabis.calendar.CalendarItem;
import club.klabis.calendar.CalendarRepository;
import club.klabis.calendar.EventCalendarItem;
import club.klabis.events.domain.Event;
import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

@SecondaryAdapter
interface CalendarInMemoryRepository extends CalendarRepository, InMemoryRepository<CalendarItem, CalendarItem.Id> {

    @Override
    default Optional<EventCalendarItem> findEventItem(Event.Id eventId, EventCalendarItem.Type itemType) {
        return findAll(it -> {
            if (it instanceof EventCalendarItem typed) {
                return eventId.equals(typed.getEventId()) && itemType.equals(typed.getType());
            } else {
                return false;
            }
        }).stream().map(EventCalendarItem.class::cast).findFirst();
    }

    @Override
    default Collection<CalendarItem> getCalendar(Calendar.CalendarPeriod calendarPeriod) {
        return findAll(new FindPredicate(calendarPeriod));
    }

    @Override
    default Calendar readCalendar(Calendar.CalendarPeriod calendarPeriod) {
        Calendar result = new Calendar(calendarPeriod, getCalendar(calendarPeriod));
        return result;
    }

}

class FindPredicate implements Predicate<CalendarItem> {

    private final Calendar.CalendarPeriod period;

    FindPredicate(Calendar.CalendarPeriod period) {
        this.period = period;
    }

    @Override
    public boolean test(CalendarItem calendarItem) {
        return period.includes(calendarItem.getStart()) || period.includes(calendarItem.getEnd());
    }
}