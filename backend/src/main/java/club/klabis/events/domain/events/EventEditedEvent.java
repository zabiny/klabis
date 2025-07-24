package club.klabis.events.domain.events;

import club.klabis.shared.domain.AggregateDomainEvent;
import club.klabis.events.domain.Event;

@org.jmolecules.event.annotation.DomainEvent
public class EventEditedEvent extends AggregateDomainEvent<Event> {
    public EventEditedEvent(Event aggregate) {
        super(aggregate);
    }
}


