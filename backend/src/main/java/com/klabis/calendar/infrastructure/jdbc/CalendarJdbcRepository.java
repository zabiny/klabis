package com.klabis.calendar.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
interface CalendarJdbcRepository extends CrudRepository<CalendarMemento, UUID> {

    @Query("""
            SELECT * FROM calendar_items
            WHERE start_date <= :endDate AND end_date >= :startDate
            ORDER BY start_date ASC, name ASC
            """)
    List<CalendarMemento> findByDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT * FROM calendar_items
            WHERE event_id = :eventId
            """)
    List<CalendarMemento> findByEventId(@Param("eventId") UUID eventId);
}
