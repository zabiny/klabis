package com.klabis.events.eventtype.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
class EventTypeManagementService implements EventTypeManagementPort {

    private static final Logger log = LoggerFactory.getLogger(EventTypeManagementService.class);
    private static final int MAX_AFFECTED_EVENTS_IN_ERROR = 5;

    private final EventTypeRepository eventTypeRepository;
    private final Optional<OrisApiClient> orisApiClient;

    EventTypeManagementService(EventTypeRepository eventTypeRepository, Optional<OrisApiClient> orisApiClient) {
        this.eventTypeRepository = eventTypeRepository;
        this.orisApiClient = orisApiClient;
    }

    @Transactional
    @Override
    public EventType createEventType(EventType.CreateEventType command) {
        if (eventTypeRepository.existsByNameIgnoreCase(command.name())) {
            throw new EventTypeNameAlreadyExistsException(command.name());
        }
        validateNoDisciplineIdConflict(command.orisDisciplineIds(), null);
        int nextSortOrder = eventTypeRepository.findMaxSortOrder() + 1;
        EventType eventType = EventType.create(command, nextSortOrder);
        return eventTypeRepository.save(eventType);
    }

    @Transactional
    @Override
    public void updateEventType(EventTypeId id, EventType.UpdateEventType command) {
        EventType eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new EventTypeNotFoundException(id));

        if (!eventType.getName().equalsIgnoreCase(command.name())
                && eventTypeRepository.existsByNameIgnoreCase(command.name())) {
            throw new EventTypeNameAlreadyExistsException(command.name());
        }

        validateNoDisciplineIdConflict(command.orisDisciplineIds(), eventType);
        eventType.update(command);
        eventTypeRepository.save(eventType);
    }

    private void validateNoDisciplineIdConflict(Set<Integer> disciplineIds, EventType ownerOrNull) {
        if (disciplineIds == null || disciplineIds.isEmpty()) {
            return;
        }
        for (int disciplineId : disciplineIds) {
            eventTypeRepository.findByOrisDisciplineId(disciplineId).ifPresent(existing -> {
                if (ownerOrNull == null || !existing.getId().equals(ownerOrNull.getId())) {
                    throw new OrisDisciplineAlreadyMappedException(disciplineId);
                }
            });
        }
    }

    @Transactional
    @Override
    public void deleteEventType(EventTypeId id) {
        eventTypeRepository.findById(id)
                .orElseThrow(() -> new EventTypeNotFoundException(id));

        if (eventTypeRepository.existsEventReferencingType(id)) {
            List<String> affectedNames = eventTypeRepository.findEventNamesReferencingType(id, MAX_AFFECTED_EVENTS_IN_ERROR);
            throw new EventTypeInUseException(id, affectedNames);
        }

        eventTypeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public EventType getEventType(EventTypeId id) {
        return eventTypeRepository.findById(id)
                .orElseThrow(() -> new EventTypeNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventType> listAllSorted() {
        return eventTypeRepository.findAllSorted();
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> listDisciplineOptions() {
        if (orisApiClient.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return orisApiClient.get().listDisciplines().payload()
                    .map(disciplines -> disciplines.values().stream()
                            .map(entry -> Integer.parseInt(entry.id()))
                            .sorted()
                            .map(String::valueOf)
                            .toList())
                    .orElse(Collections.emptyList());
        } catch (RuntimeException e) {
            log.warn("ORIS discipline list unavailable, returning empty options", e);
            return Collections.emptyList();
        }
    }
}
