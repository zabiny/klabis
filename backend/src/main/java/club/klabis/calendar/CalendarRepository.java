package club.klabis.calendar;

import club.klabis.events.domain.Event;
import club.klabis.shared.application.DataRepository;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
@SecondaryPort
public interface CalendarRepository extends DataRepository<CalendarItem, CalendarItem.Id> {

    Optional<EventCalendarItem> findEventItem(Event.Id eventId, EventCalendarItem.Type itemType);

    Collection<CalendarItem> getCalendar(Calendar.CalendarPeriod calendarPeriod);

    Calendar readCalendar(Calendar.CalendarPeriod calendarPeriod);
}
