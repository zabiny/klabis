package com.klabis.membershipfees.infrastructure;

import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.events.application.EventTypeOptionsProvider;
import com.klabis.membershipfees.application.EventTypeOptionsPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@SecondaryAdapter
@Component
class EventTypeOptionsAdapter implements EventTypeOptionsPort {

    private final EventTypeOptionsProvider eventTypeOptionsProvider;

    EventTypeOptionsAdapter(EventTypeOptionsProvider eventTypeOptionsProvider) {
        this.eventTypeOptionsProvider = eventTypeOptionsProvider;
    }

    @Override
    public List<HalFormsInlineOption> listEventTypeOptions() {
        return eventTypeOptionsProvider.listEventTypeOptions();
    }
}
