package com.klabis.events.eventtype.application;

import com.klabis.events.EventTypeId;
import com.klabis.events.eventtype.domain.*;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class EventTypeManagementService implements EventTypeManagementPort {

    private static final int MAX_AFFECTED_EVENTS_IN_ERROR = 5;

    private final EventTypeRepository eventTypeRepository;

    EventTypeManagementService(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    @Transactional
    @Override
    public EventType createEventType(EventType.CreateEventType command) {
        if (eventTypeRepository.existsByNameIgnoreCase(command.name())) {
            throw new EventTypeNameAlreadyExistsException(command.name());
        }
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

        eventType.update(command);
        eventTypeRepository.save(eventType);
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
}
