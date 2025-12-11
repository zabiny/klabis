package club.klabis.calendar;

import club.klabis.shared.config.Globals;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@AggregateRoot
public class Calendar extends AbstractAggregateRoot<Calendar> {

    @Identity
    private CalendarPeriod period;
    private Set<CalendarItem> items = new HashSet<>();

    public enum CalendarType {
        DAY, MONTH, YEAR
    }

    public record CalendarPeriod(LocalDate periodStart, LocalDate periodEnd) {

        public CalendarPeriod {
            Assert.isTrue(periodStart.isBefore(periodEnd), "Start date must be before end date");
        }

        public boolean includes(LocalDate date) {
            return periodStart.isBefore(date) && periodEnd.isAfter(date);
        }

        public boolean includes(ZonedDateTime date) {
            return includes(Globals.toLocalDate(date));
        }

        public static CalendarPeriod forType(@NonNull CalendarType type, @Nullable LocalDate referenceDate) {
            return switch (type) {
                case DAY -> forDay(referenceDate);
                case MONTH -> forMonth(referenceDate);
                case YEAR -> forYear(referenceDate);
            };
        }

        public static CalendarPeriod forDay(@Nullable LocalDate referenceDate) {
            if (referenceDate == null) {
                referenceDate = LocalDate.now();
            }
            return forPeriod(referenceDate, referenceDate.plusDays(1));
        }

        public static CalendarPeriod forYear(@Nullable LocalDate referenceDate) {
            if (referenceDate == null) {
                referenceDate = LocalDate.now();
            }
            return forPeriod(referenceDate.with(TemporalAdjusters.firstDayOfYear()),
                    referenceDate.with(TemporalAdjusters.lastDayOfYear()));
        }

        public static CalendarPeriod forMonth(@Nullable LocalDate referenceDate) {
            if (referenceDate == null) {
                referenceDate = LocalDate.now();
            }
            return forPeriod(referenceDate.with(TemporalAdjusters.firstDayOfMonth()),
                    referenceDate.with(TemporalAdjusters.lastDayOfMonth()));
        }

        public static CalendarPeriod forPeriod(@NonNull LocalDate periodStart, @NonNull LocalDate periodEnd) {
            return new CalendarPeriod(periodStart, periodEnd);
        }

    }


    public Calendar(CalendarPeriod period, Collection<CalendarItem> items) {
        this.period = period;
        this.items.addAll(items);
    }

    public CalendarItem handle(CreateCalendarItemCommand command) {
        if (!period.includes(command.start()) && !period.includes(command.end())) {
            throw new IllegalArgumentException("Requested item is not placed in this calendar");
        }

        CalendarItem result = CalendarItem.calendarItem(
                        Globals.toZonedDateTime(command.start()),
                        Globals.toZonedDateTime(command.end()))
                .withNote(command.note());
        this.items.add(result);
        return result;

    }

    public Set<CalendarItem> getItems() {
        return Collections.unmodifiableSet(items);
    }

    public LocalDate getPeriodEnd() {
        return period.periodEnd;
    }

    public LocalDate getPeriodStart() {
        return period.periodStart;
    }
}

