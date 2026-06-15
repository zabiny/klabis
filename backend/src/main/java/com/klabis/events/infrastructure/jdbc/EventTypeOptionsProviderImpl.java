package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.events.application.EventTypeOptionsProvider;
import com.klabis.events.domain.EventTypeRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@SecondaryAdapter
class EventTypeOptionsProviderImpl implements EventTypeOptionsProvider {

    private final EventTypeRepository eventTypeRepository;

    EventTypeOptionsProviderImpl(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    @Override
    public List<HalFormsInlineOption> listEventTypeOptions() {
        return eventTypeRepository.findAllSorted().stream()
                .map(eventType -> new HalFormsInlineOption(
                        eventType.getId().value().toString(),
                        eventType.getName()))
                .toList();
    }
}
