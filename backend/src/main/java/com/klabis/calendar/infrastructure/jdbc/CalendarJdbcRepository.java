package com.klabis.calendar.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
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
            WHERE kind IN (:kinds)
              AND start_date <= :endDate
              AND end_date >= :startDate
            ORDER BY start_date ASC, name ASC
            """)
    List<CalendarMemento> findByDateRangeAndKinds(@Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("kinds") Collection<String> kinds);

    @Query("""
            SELECT * FROM calendar_items
            WHERE event_id IN (:eventIds)
              AND start_date <= :endDate
              AND end_date >= :startDate
            ORDER BY start_date ASC, name ASC
            """)
    List<CalendarMemento> findByDateRangeAndEventIds(@Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate,
                                                      @Param("eventIds") Collection<UUID> eventIds);

    @Query("""
            SELECT * FROM calendar_items
            WHERE kind IN (:kinds)
              AND event_id IN (:eventIds)
              AND start_date <= :endDate
              AND end_date >= :startDate
            ORDER BY start_date ASC, name ASC
            """)
    List<CalendarMemento> findByDateRangeAndKindsAndEventIds(@Param("startDate") LocalDate startDate,
                                                              @Param("endDate") LocalDate endDate,
                                                              @Param("kinds") Collection<String> kinds,
                                                              @Param("eventIds") Collection<UUID> eventIds);

    @Query("""
            SELECT * FROM calendar_items
            WHERE event_id = :eventId
            """)
    List<CalendarMemento> findByEventId(@Param("eventId") UUID eventId);
}
