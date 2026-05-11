package com.klabis.events.eventtype.infrastructure.jdbc;

import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.domain.EventType;
import com.klabis.events.eventtype.domain.EventTypeRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@org.jmolecules.ddd.annotation.Repository
class EventTypeRepositoryAdapter implements EventTypeRepository {

    private final EventTypeJdbcRepository jdbcRepository;

    EventTypeRepositoryAdapter(EventTypeJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public EventType save(EventType eventType) {
        return jdbcRepository.save(EventTypeMemento.from(eventType)).toEventType();
    }

    @Override
    public Optional<EventType> findById(EventTypeId id) {
        return jdbcRepository.findById(id.value()).map(EventTypeMemento::toEventType);
    }

    @Override
    public List<EventType> findAllSorted() {
        return jdbcRepository.findAllOrderedBySortOrder().stream()
                .map(EventTypeMemento::toEventType)
                .toList();
    }

    @Override
    public Optional<EventType> findByNameIgnoreCase(String name) {
        return jdbcRepository.findByNameIgnoreCase(name).map(EventTypeMemento::toEventType);
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return jdbcRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public void deleteById(EventTypeId id) {
        jdbcRepository.deleteById(id.value());
    }

    @Override
    public int findMaxSortOrder() {
        return jdbcRepository.findMaxSortOrder();
    }

    @Override
    public boolean existsEventReferencingType(EventTypeId id) {
        return jdbcRepository.existsEventReferencingType(id.value());
    }

    @Override
    public List<String> findEventNamesReferencingType(EventTypeId id, int limit) {
        return jdbcRepository.findEventNamesReferencingType(id.value(), limit);
    }
}
