package club.klabis.domain.events.events;

import club.klabis.domain.AggregateDomainEvent;
import club.klabis.domain.events.Event;

@org.jmolecules.event.annotation.DomainEvent
public class EventEditedEvent extends AggregateDomainEvent<Event> {
    public EventEditedEvent(Event aggregate) {
        super(aggregate);
    }
}


