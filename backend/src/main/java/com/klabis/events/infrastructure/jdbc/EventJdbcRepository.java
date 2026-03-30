package com.klabis.events.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JDBC repository for Event aggregate using Memento pattern.
 * <p>
 * This repository manages {@link EventMemento} instances, which act as persistence adapters for the
 * pure domain Event entity.
 * <p>
 * Note: This interface does NOT implement EventRepository directly.
 * Instead, EventRepositoryAdapter wraps this repository and implements EventRepository.
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

    @Query("SELECT EXISTS(SELECT 1 FROM events WHERE oris_id = :orisId)")
    boolean existsByOrisId(@Param("orisId") int orisId);

    // findAll(Pageable) is inherited from PagingAndSortingRepository
    // findById(UUID) is inherited from CrudRepository
    // save() is inherited from CrudRepository
}
