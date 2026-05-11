package com.klabis.events.eventtype.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface EventTypeJdbcRepository extends CrudRepository<EventTypeMemento, UUID> {

    @Query("SELECT * FROM event_types ORDER BY sort_order ASC, name ASC")
    List<EventTypeMemento> findAllOrderedBySortOrder();

    @Query("SELECT * FROM event_types WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    Optional<EventTypeMemento> findByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT COUNT(*) > 0 FROM event_types WHERE LOWER(name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM event_types")
    int findMaxSortOrder();

    @Query("SELECT COUNT(*) > 0 FROM events WHERE event_type_id = :eventTypeId")
    boolean existsEventReferencingType(@Param("eventTypeId") UUID eventTypeId);

    @Query("SELECT name FROM events WHERE event_type_id = :eventTypeId ORDER BY name LIMIT :limit")
    List<String> findEventNamesReferencingType(@Param("eventTypeId") UUID eventTypeId, @Param("limit") int limit);
}
