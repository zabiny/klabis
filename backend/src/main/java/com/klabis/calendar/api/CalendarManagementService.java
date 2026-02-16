package com.klabis.calendar.api;

import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.persistence.CalendarRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    /**
     * Lists calendar items with pagination and optional date range filtering.
     * <p>
     * If startDate and endDate are provided, returns items that intersect
     * with the specified date range. Otherwise, returns all items.
     *
     * @param startDate optional start date for filtering (inclusive)
     * @param endDate   optional end date for filtering (inclusive)
     * @param pageable  pagination and sorting parameters
     * @return page of calendar item DTOs
     */
    @Transactional(readOnly = true)
    public Page<CalendarItemDto> listCalendarItems(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        List<CalendarItem> items;

        if (startDate != null && endDate != null) {
            items = calendarRepository.findByDateRange(startDate, endDate);
        } else {
            items = List.of();
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), items.size());
        List<CalendarItemDto> pageContent = items.subList(start, end).stream()
                .map(this::mapToDto)
                .toList();

        return new PageImpl<>(pageContent, pageable, items.size());
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
