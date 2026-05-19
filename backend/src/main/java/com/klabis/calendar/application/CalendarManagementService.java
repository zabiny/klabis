package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.calendar.domain.ManualCalendarItem;
import com.klabis.events.EventId;
import com.klabis.events.EventScheduleQuery;
import com.klabis.members.MemberId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
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
    public List<CalendarItem> listCalendarItems(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Sort sort,
                                                @Nullable MemberId myScheduleMemberId) {
        validateDateRange(startDate, endDate);
        List<CalendarItem> all = calendarRepository.findByDateRange(startDate, endDate);
        if (myScheduleMemberId == null) {
            return all;
        }
        return filterForMySchedule(all, myScheduleMemberId, startDate, endDate);
    }

    private List<CalendarItem> filterForMySchedule(List<CalendarItem> items, MemberId memberId,
                                                    LocalDate from, LocalDate to) {
        Set<EventId> registrationIds = eventScheduleQuery.findEventIdsByRegistration(memberId, from, to);
        Set<EventId> coordinatorIds = eventScheduleQuery.findEventIdsByCoordinator(memberId, from, to);

        Set<EventId> myEventIds = new HashSet<>();
        myEventIds.addAll(registrationIds);
        myEventIds.addAll(coordinatorIds);

        return items.stream()
                .filter(item -> item instanceof EventCalendarItem eventItem
                        && eventItem.getKind() == CalendarItemKind.EVENT_DATE
                        && myEventIds.contains(eventItem.getEventId()))
                .toList();
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
