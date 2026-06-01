package com.klabis.events.eventtype.infrastructure.jdbc;

import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.domain.EventType;
import com.klabis.events.eventtype.domain.EventTypeRepository;
import com.klabis.events.eventtype.domain.OrisDisciplineAlreadyMappedException;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.dao.DataIntegrityViolationException;

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
        try {
            return jdbcRepository.save(EventTypeMemento.from(eventType)).toEventType();
        } catch (DataIntegrityViolationException e) {
            if (isDisciplineConstraintViolation(e)) {
                // Translate DB unique constraint on discipline_id to a domain exception so the
                // race-condition path (TOCTOU between app-layer check and save) surfaces correctly.
                throw new OrisDisciplineAlreadyMappedException(-1);
            }
            throw e;
        }
    }

    private static boolean isDisciplineConstraintViolation(DataIntegrityViolationException e) {
        Throwable cause = e;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("event_type_oris_disciplines") || lower.contains("idx_event_type_oris_disciplines_discipline")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
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

    @Override
    public Optional<EventType> findByOrisDisciplineId(int disciplineId) {
        // Custom @Query does not trigger @MappedCollection loading in Spring Data JDBC.
        // Reload the full aggregate via findById to ensure orisDisciplineIds is populated.
        return jdbcRepository.findByOrisDisciplineId(disciplineId)
                .map(m -> jdbcRepository.findById(m.getId()).orElseThrow())
                .map(EventTypeMemento::toEventType);
    }
}
