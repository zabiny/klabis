package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarFilter;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.calendar.domain.ManualCalendarItem;
import com.klabis.events.EventId;
import com.klabis.events.application.EventScheduleQuery;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
class CalendarManagementService implements CalendarManagementPort {

    private final CalendarRepository calendarRepository;
    private final EventScheduleQuery eventScheduleQuery;

    private static final int MAX_DATE_RANGE_DAYS = 366;

    public CalendarManagementService(CalendarRepository calendarRepository, EventScheduleQuery eventScheduleQuery) {
        this.calendarRepository = calendarRepository;
        this.eventScheduleQuery = eventScheduleQuery;
    }

    @Transactional(readOnly = true)
    @Override
    public List<CalendarItem> listCalendarItems(LocalDate startDate, LocalDate endDate, Sort sort,
                                                boolean myScheduleRequested, @Nullable MemberId myScheduleMemberId) {
        validateDateRange(startDate, endDate);

        if (!myScheduleRequested) {
            return calendarRepository.findByFilter(CalendarFilter.dateRange(startDate, endDate), sort);
        }

        if (myScheduleMemberId == null) {
            return List.of();
        }

        Set<EventId> myEventIds = eventScheduleQuery.findEventIdsForMemberSchedule(myScheduleMemberId, startDate, endDate);

        if (myEventIds.isEmpty()) {
            return List.of();
        }

        CalendarFilter filter = CalendarFilter.dateRange(startDate, endDate)
                .withItemTypes(Set.of(CalendarItemKind.EVENT_DATE))
                .withEventIds(myEventIds);

        return calendarRepository.findByFilter(filter, sort);
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
    public CalendarItem createCalendarItem(CalendarItem.CreateCalendarItem command) {
        ManualCalendarItem calendarItem = ManualCalendarItem.create(command);
        return calendarRepository.save(calendarItem);
    }

    @Transactional
    @Override
    public void updateCalendarItem(CalendarItemId calendarItemId, CalendarItem.UpdateCalendarItem command) {
        CalendarItem calendarItem = calendarRepository.findById(calendarItemId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId.value()));

        if (!(calendarItem instanceof ManualCalendarItem manual)) {
            throw new com.klabis.calendar.domain.CalendarItemReadOnlyException();
        }

        manual.update(command);

        calendarRepository.save(manual);
    }

    @Transactional
    @Override
    public void deleteCalendarItem(CalendarItemId calendarItemId) {
        CalendarItem calendarItem = calendarRepository.findById(calendarItemId)
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId.value()));

        calendarItem.assertCanBeDeleted();

        calendarRepository.delete(calendarItem);
    }
}
