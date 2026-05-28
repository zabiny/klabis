package com.klabis.events.application;

import com.klabis.events.domain.EventRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

@Service
class ImportedOrisEventsService implements ImportedOrisEventsPort {

    private final EventRepository eventRepository;

    ImportedOrisEventsService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Set<Integer> findImportedOrisIds(Collection<Integer> candidateOrisIds) {
        return eventRepository.findImportedOrisIds(candidateOrisIds);
    }
}
