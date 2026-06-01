package com.klabis.events.eventtype.domain;

import com.klabis.events.EventTypeId;

import java.util.List;
import java.util.Optional;

public interface EventTypeRepository {

    EventType save(EventType eventType);

    Optional<EventType> findById(EventTypeId id);

    List<EventType> findAllSorted();

    Optional<EventType> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    void deleteById(EventTypeId id);

    int findMaxSortOrder();

    boolean existsEventReferencingType(EventTypeId id);

    List<String> findEventNamesReferencingType(EventTypeId id, int limit);

    Optional<EventType> findByOrisDisciplineId(int disciplineId);
}
