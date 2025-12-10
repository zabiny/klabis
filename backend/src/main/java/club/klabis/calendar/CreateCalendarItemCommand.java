package club.klabis.calendar;

import java.time.LocalDate;

public record CreateCalendarItemCommand(LocalDate start, LocalDate end, String note) {

    public static CreateCalendarItemCommand task(LocalDate date, String note) {
        return new CreateCalendarItemCommand(date, date, note);
    }

    public Calendar.CalendarPeriod getPeriod() {
        return Calendar.CalendarPeriod.forPeriod(start, end);
    }

}
