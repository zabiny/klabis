package club.klabis.calendar;

import club.klabis.shared.config.Globals;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
public class CalendarItem {
    @Identity
    private Id id;
    private ZonedDateTime start;
    private ZonedDateTime end;
    private String note;

    public static Id id(long value) {
        return new Id(value);
    }

    public record Id(long value) {
        static long MAX_VALUE = 0L;

        static synchronized Id newId() {
            return new Id(MAX_VALUE++);
        }

        @Override
        public String toString() {
            return "CALENDARITEM_%d".formatted(value);
        }
    }

    public static CalendarItem calendarItem(ZonedDateTime start, ZonedDateTime end) {
        return new CalendarItem(Id.newId(), start, end, null);
    }

    public static CalendarItem task(LocalDate day, String note) {
        return calendarItem(day.atStartOfDay(Globals.KLABIS_ZONE),
                day.atStartOfDay(Globals.KLABIS_ZONE)).withNote(note);
    }

    protected CalendarItem() {

    }

    protected CalendarItem(Id id, ZonedDateTime start, ZonedDateTime end, String note) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.note = note;
    }

    public CalendarItem withNote(String note) {
        this.note = note;
        return this;
    }

    public static CalendarItem todayTask(String note) {
        return CalendarItem.calendarItem(ZonedDateTime.now(), ZonedDateTime.now()).withNote(note);
    }

    public Id getId() {
        return id;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public ZonedDateTime getEnd() {
        return end;
    }

    public String getNote() {
        return note;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CalendarItem) obj;
        return this.id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CalendarItem[" + "id=" + id + ", " + "start=" + start + ", " + "end=" + end + ", " + "note=" + note + ']';
    }


}
