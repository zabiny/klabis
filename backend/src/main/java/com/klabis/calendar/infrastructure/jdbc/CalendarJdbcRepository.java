package com.klabis.calendar.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JDBC repository for CalendarItem aggregate using Memento pattern.
 * <p>
 * This repository manages {@link CalendarMemento} instances, which act as persistence adapters for the
 * pure domain CalendarItem entity.
 * <p>
 * Note: This interface does NOT implement CalendarRepository directly.
 * Instead, CalendarRepositoryAdapter wraps this repository and implements CalendarRepository.
 * This is necessary because Spring Data JDBC repositories cannot extend custom interfaces
 * with different ID types (CalendarItemId vs UUID).
 * <p>
 * The memento pattern ensures:
 * - CalendarItem entity remains a pure domain object without Spring annotations
 * - All JDBC persistence concerns are handled by CalendarMemento
 */
@Repository
interface CalendarJdbcRepository extends CrudRepository<CalendarMemento, UUID> {

    /**
     * Find calendar items that intersect with the specified date range.
     * <p>
     * Date range intersection logic:
     * - Item's start_date <= range end_date (item starts before or on range end)
     * - Item's end_date >= range start_date (item ends on or after range start)
     * <p>
     * This handles multi-day items that span across the boundary dates.
     *
     * @param startDate start of the query range (inclusive)
     * @param endDate   end of the query range (inclusive)
     * @return list of calendar item mementos intersecting the date range
     */
    @Query("""
            SELECT * FROM calendar_items
            WHERE start_date <= :endDate AND end_date >= :startDate
            ORDER BY start_date ASC, name ASC
            """)
    List<CalendarMemento> findByDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * Find a calendar item by linked event ID.
     * <p>
     * Used by event handlers to locate calendar items synchronized from events.
     *
     * @param eventId the event ID (UUID)
     * @return Optional containing the calendar item memento if found, empty otherwise
     */
    Optional<CalendarMemento> findByEventId(UUID eventId);
}
