package com.klabis.events.application;

import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.events.DisciplineId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
class EventTypeManagementService implements EventTypeManagementPort {

    private static final int MAX_AFFECTED_EVENTS_IN_ERROR = 5;

    private final EventTypeRepository eventTypeRepository;
    private final DisciplineRepository disciplineRepository;

    EventTypeManagementService(EventTypeRepository eventTypeRepository, DisciplineRepository disciplineRepository) {
        this.eventTypeRepository = eventTypeRepository;
        this.disciplineRepository = disciplineRepository;
    }

    @Transactional
    @Override
    public EventType createEventType(EventType.CreateEventType command) {
        if (eventTypeRepository.existsByNameIgnoreCase(command.name())) {
            throw new EventTypeNameAlreadyExistsException(command.name());
        }
        validateNoDisciplineIdConflict(command.disciplineIds(), null);
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

        validateNoDisciplineIdConflict(command.disciplineIds(), eventType);
        eventType.update(command);
        eventTypeRepository.save(eventType);
    }

    private void validateNoDisciplineIdConflict(Set<DisciplineId> disciplineIds, EventType ownerOrNull) {
        if (disciplineIds == null || disciplineIds.isEmpty()) {
            return;
        }
        for (DisciplineId disciplineId : disciplineIds) {
            eventTypeRepository.findByDisciplineId(disciplineId).ifPresent(existing -> {
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
    public List<HalFormsInlineOption> listDisciplineOptions() {
        return disciplineRepository.findAllSorted().stream()
                .map(EventTypeManagementService::toInlineOption)
                .toList();
    }

    private static HalFormsInlineOption toInlineOption(Discipline discipline) {
        return new HalFormsInlineOption(discipline.getId().value().toString(), discipline.getName());
    }
}
