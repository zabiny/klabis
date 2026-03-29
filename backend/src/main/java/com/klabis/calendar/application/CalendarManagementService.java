package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
class CalendarManagementService implements CalendarManagementPort {

    private final CalendarRepository calendarRepository;

    private static final int MAX_DATE_RANGE_DAYS = 366; // 1 year (including leap year)

    public CalendarManagementService(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public List<CalendarItem> listCalendarItems(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Sort sort) {
        validateDateRange(startDate, endDate);
        return calendarRepository.findByDateRange(startDate, endDate);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    "Date range must not exceed %d days. Requested range: %d days".formatted(
                            MAX_DATE_RANGE_DAYS, daysBetween)
            );
        }
    }

    @Transactional(readOnly = true)
    @Override
    public CalendarItem getCalendarItem(CalendarItemId calendarItemId) {
        return calendarRepository.findById(calendarItemId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId.value()));
    }

    @Transactional
    @Override
    public CalendarItem createCalendarItem(CalendarItemCommand command) {
        CalendarItem calendarItem = CalendarItem.create(new CalendarItem.CreateCalendarItem(
                command.name(),
                command.description(),
                command.startDate(),
                command.endDate()
        ));

        return calendarRepository.save(calendarItem);
    }

    @Transactional
    @Override
    public void updateCalendarItem(CalendarItemId calendarItemId, CalendarItemCommand command) {
        CalendarItem calendarItem = calendarRepository.findById(calendarItemId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId.value()));

        if (calendarItem.isEventLinked()) {
            throw new CalendarItemReadOnlyException();
        }

        calendarItem.update(
                command.name(),
                command.description(),
                command.startDate(),
                command.endDate()
        );

        calendarRepository.save(calendarItem);
    }

    @Transactional
    @Override
    public void deleteCalendarItem(CalendarItemId calendarItemId) {
        CalendarItem calendarItem = calendarRepository.findById(calendarItemId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId.value()));

        if (calendarItem.isEventLinked()) {
            throw new CalendarItemReadOnlyException();
        }

        calendarRepository.delete(calendarItem);
    }

}
