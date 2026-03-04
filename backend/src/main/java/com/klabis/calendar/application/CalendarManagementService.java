package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.domain.CalendarRepository;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
class CalendarManagementService implements CalendarManagementPort {

    private final CalendarRepository calendarRepository;

    public CalendarManagementService(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    private static final int MAX_DATE_RANGE_DAYS = 366; // 1 year (including leap year)

    @Transactional(readOnly = true)
    @Override
    public List<CalendarItem> listCalendarItems(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Sort sort) {
        validateDateRange(startDate, endDate);

        List<CalendarItem> items = calendarRepository.findByDateRange(startDate, endDate);

        Comparator<CalendarItem> comparator;
        if (sort.isSorted() && sort.iterator().hasNext()) {
            Sort.Order order = sort.iterator().next();
            comparator = getComparatorForField(order.getProperty());
            if (order.isDescending()) {
                comparator = comparator.reversed();
            }
        } else {
            comparator = Comparator.comparing(CalendarItem::getStartDate);
        }

        return items.stream()
                .sorted(comparator)
                .toList();
    }

    private Comparator<CalendarItem> getComparatorForField(String field) {
        return switch (field) {
            case "id" -> Comparator.comparing(item -> item.getId().value());
            case "name" -> Comparator.comparing(CalendarItem::getName);
            case "startDate" -> Comparator.comparing(CalendarItem::getStartDate);
            case "endDate" -> Comparator.comparing(CalendarItem::getEndDate);
            default -> throw new IllegalArgumentException("Invalid sort field: " + field);
        };
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    String.format("Date range must not exceed %d days. Requested range: %d days",
                            MAX_DATE_RANGE_DAYS, daysBetween)
            );
        }
    }

    @Transactional(readOnly = true)
    @Override
    public CalendarItem getCalendarItem(UUID calendarItemId) {
        return calendarRepository.findById(new CalendarItemId(calendarItemId))
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId));
    }

    @Transactional
    @Override
    public CalendarItem createCalendarItem(CreateCalendarItemCommand command) {
        CalendarItem calendarItem = CalendarItem.create(
                command.name(),
                command.description(),
                command.startDate(),
                command.endDate()
        );

        return calendarRepository.save(calendarItem);
    }

    @Transactional
    @Override
    public void updateCalendarItem(UUID calendarItemId, UpdateCalendarItemCommand command) {
        CalendarItem calendarItem = calendarRepository.findById(new CalendarItemId(calendarItemId))
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId));

        try {
            calendarItem.update(
                    command.name(),
                    command.description(),
                    command.startDate(),
                    command.endDate()
            );
        } catch (com.klabis.common.exceptions.BusinessRuleViolationException e) {
            throw new CalendarItemReadOnlyException();
        }

        calendarRepository.save(calendarItem);
    }

    @Transactional
    @Override
    public void deleteCalendarItem(UUID calendarItemId) {
        CalendarItem calendarItem = calendarRepository.findById(new CalendarItemId(calendarItemId))
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId));

        try {
            calendarItem.delete();
        } catch (com.klabis.common.exceptions.BusinessRuleViolationException e) {
            throw new CalendarItemReadOnlyException();
        }

        calendarRepository.delete(calendarItem);
    }

}
