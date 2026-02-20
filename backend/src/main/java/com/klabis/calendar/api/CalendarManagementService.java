package com.klabis.calendar.api;

import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.persistence.CalendarRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Application service for calendar item management operations.
 * <p>
 * Handles manual calendar item creation, updates, deletion, and queries.
 * All mutation operations are transactional.
 * <p>
 * Event-linked calendar items (eventId != null) are read-only and managed
 * automatically through event handlers.
 * <p>
 * This is an application service (not a port), responsible for orchestrating
 * use cases exposed through the REST API.
 */
@Service
class CalendarManagementService {

    private final CalendarRepository calendarRepository;

    public CalendarManagementService(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    private static final int MAX_DATE_RANGE_DAYS = 366; // 1 year (including leap year)

    /**
     * Lists calendar items with date range filtering.
     * <p>
     * Returns items that intersect with the specified date range, sorted according to the provided sort parameter.
     * Maximum date range is 1 year (366 days).
     *
     * @param startDate start date for filtering (inclusive)
     * @param endDate   end date for filtering (inclusive)
     * @param sort      sort specification
     * @return list of calendar item DTOs
     * @throws IllegalArgumentException if date range exceeds 366 days
     */
    @Transactional(readOnly = true)
    public List<CalendarItemDto> listCalendarItems(@NonNull LocalDate startDate, @NonNull LocalDate endDate, Sort sort) {
        validateDateRange(startDate, endDate);

        List<CalendarItem> items = calendarRepository.findByDateRange(startDate, endDate);

        // Apply sorting based on sort parameter
        Comparator<CalendarItem> comparator;
        if (sort.isSorted() && sort.iterator().hasNext()) {
            Sort.Order order = sort.iterator().next();
            comparator = getComparatorForField(order.getProperty());
            if (order.isDescending()) {
                comparator = comparator.reversed();
            }
        } else {
            // Default sorting by startDate ascending
            comparator = Comparator.comparing(CalendarItem::getStartDate);
        }

        return items.stream()
                .sorted(comparator)
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Gets comparator for specified field name.
     *
     * @param field field name to sort by
     * @return comparator for that field
     */
    private Comparator<CalendarItem> getComparatorForField(String field) {
        return switch (field) {
            case "id" -> Comparator.comparing(item -> item.getId().value());
            case "name" -> Comparator.comparing(CalendarItem::getName);
            case "startDate" -> Comparator.comparing(CalendarItem::getStartDate);
            case "endDate" -> Comparator.comparing(CalendarItem::getEndDate);
            default -> throw new IllegalArgumentException("Invalid sort field: " + field);
        };
    }

    /**
     * Validates that the date range is within acceptable limits.
     *
     * @param startDate start date
     * @param endDate   end date
     * @throws IllegalArgumentException if range exceeds 366 days
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1; // Include both start and end dates

        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            throw new IllegalArgumentException(
                    String.format("Date range must not exceed %d days. Requested range: %d days",
                            MAX_DATE_RANGE_DAYS, daysBetween)
            );
        }
    }

    /**
     * Retrieves calendar item details by ID.
     *
     * @param calendarItemId the ID of the calendar item
     * @return calendar item DTO
     * @throws CalendarNotFoundException if calendar item not found
     */
    @Transactional(readOnly = true)
    public CalendarItemDto getCalendarItem(UUID calendarItemId) {
        CalendarItem calendarItem = calendarRepository.findById(new CalendarItemId(calendarItemId))
                .orElseThrow(() -> new CalendarNotFoundException(calendarItemId));

        return mapToDto(calendarItem);
    }

    /**
     * Creates a new manual calendar item.
     * <p>
     * Manual calendar items (eventId = null) can be freely updated and deleted.
     *
     * @param command the create calendar item command
     * @return the ID of the created calendar item
     */
    @Transactional
    public UUID createCalendarItem(CreateCalendarItemCommand command) {
        CalendarItem calendarItem = CalendarItem.create(
                command.name(),
                command.description(),
                command.startDate(),
                command.endDate()
        );

        CalendarItem savedItem = calendarRepository.save(calendarItem);
        return savedItem.getId().value();
    }

    /**
     * Updates an existing manual calendar item.
     * <p>
     * Business rule: Only manual items (eventId == null) can be updated.
     * Event-linked items are read-only and managed automatically.
     *
     * @param calendarItemId the ID of the calendar item to update
     * @param command        the update calendar item command
     * @throws CalendarNotFoundException        if calendar item not found
     * @throws CalendarItemReadOnlyException    if calendar item is event-linked
     * @throws IllegalArgumentException         if validation fails
     */
    @Transactional
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

    /**
     * Deletes a manual calendar item.
     * <p>
     * Business rule: Only manual items (eventId == null) can be deleted.
     * Event-linked items are read-only and managed automatically.
     *
     * @param calendarItemId the ID of the calendar item to delete
     * @throws CalendarNotFoundException        if calendar item not found
     * @throws CalendarItemReadOnlyException    if calendar item is event-linked
     */
    @Transactional
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

    // ========== Mapping Methods ==========

    /**
     * Maps CalendarItem domain object to CalendarItemDto.
     *
     * @param calendarItem the calendar item to map
     * @return calendar item DTO
     */
    private CalendarItemDto mapToDto(CalendarItem calendarItem) {
        return new CalendarItemDto(
                calendarItem.getId().value(),
                calendarItem.getName(),
                calendarItem.getDescription(),
                calendarItem.getStartDate(),
                calendarItem.getEndDate(),
                calendarItem.getEventId() != null ? calendarItem.getEventId().value() : null
        );
    }
}
