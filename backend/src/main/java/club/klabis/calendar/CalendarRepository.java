package club.klabis.calendar;

import club.klabis.shared.application.DataRepository;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Collection;

@Repository
@SecondaryPort
public interface CalendarRepository extends DataRepository<CalendarItem, CalendarItem.Id> {

    Collection<CalendarItem> getCalendar(Calendar.CalendarPeriod calendarPeriod);

    Calendar readCalendar(Calendar.CalendarPeriod calendarPeriod);
}
