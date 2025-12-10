package club.klabis.calendar.infrastructure;

import club.klabis.calendar.Calendar;
import club.klabis.calendar.CalendarItem;
import club.klabis.calendar.CalendarRepository;
import club.klabis.calendar.CreateCalendarItemCommand;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

@Service
@Component
public class CalendarService {

    private final CalendarRepository calendarRepository;

    public CalendarService(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    public Collection<CalendarItem> getCalendarItems(Calendar.CalendarType calendarType, LocalDate referenceDate) {
        return calendarRepository.getCalendar(Calendar.CalendarPeriod.forType(calendarType, referenceDate));
    }

    public Optional<CalendarItem> getCalendarItem(CalendarItem.Id calendarItemId) {
        return calendarRepository.findById(calendarItemId);
    }

    @Transactional
    public CalendarItem createCalendarItem(CreateCalendarItemCommand command) {
        Calendar c = calendarRepository.readCalendar(command.getPeriod());

        return calendarRepository.save(c.handle(command));
    }

}
