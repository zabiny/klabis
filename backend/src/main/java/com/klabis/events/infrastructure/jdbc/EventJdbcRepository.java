package com.klabis.events.infrastructure.jdbc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JDBC repository for Event aggregate using Memento pattern.
 * <p>
 * This repository manages {@link EventMemento} instances, which act as persistence adapters for the
 * pure domain Event entity.
 * <p>
 * Note: This interface does NOT implement EventRepository directly.
 * Instead, EventRepositoryJdbcImpl wraps this repository and implements EventRepository.
 * This is necessary because Spring Data JDBC repositories cannot extend custom interfaces
 * with different ID types (EventId vs UUID).
 * <p>
 * The memento pattern ensures:
 * - Event entity remains a pure domain object without Spring annotations
 * - All JDBC persistence concerns are handled by EventMemento
 * - Domain events are still published via Spring Modulith's outbox pattern
 */
@Repository
interface EventJdbcRepository extends CrudRepository<EventMemento, UUID>, PagingAndSortingRepository<EventMemento, UUID> {

    /**
     * Find events by status with pagination.
     *
     * @param status   the event status as string
     * @param pageable pagination parameters
     * @return page of event mementos
     */
    Page<EventMemento> findByStatus(String status, Pageable pageable);

    /**
     * Find events by organizer with pagination.
     *
     * @param organizer the organizer name
     * @param pageable  pagination parameters
     * @return page of event mementos
     */
    Page<EventMemento> findByOrganizer(String organizer, Pageable pageable);

    /**
     * Find events within a date range with pagination.
     * Uses derived query method - Spring Data JDBC supports Between keyword.
     *
     * @param from     start date (inclusive)
     * @param to       end date (inclusive)
     * @param pageable pagination parameters
     * @return page of event mementos
     */
    Page<EventMemento> findByEventDateBetween(LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Find active events with event date before the specified date.
     * Used by the automatic event completion scheduler.
     *
     * @param date the date to compare against
     * @return list of active event mementos with past event dates
     */
    @Query("""
            SELECT * FROM events
            WHERE status = 'ACTIVE' AND event_date < :date
            ORDER BY event_date ASC
            """)
    List<EventMemento> findActiveEventsWithDateBefore(@Param("date") LocalDate date);

    // findAll(Pageable) is inherited from PagingAndSortingRepository
    // findById(UUID) is inherited from CrudRepository
    // save() is inherited from CrudRepository
}
