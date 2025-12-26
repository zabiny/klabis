package club.klabis.events.domain.events;

import club.klabis.events.domain.Event;
import club.klabis.shared.domain.AggregateDomainEvent;

public class EventCostChangedEvent extends AggregateDomainEvent<Event> {
    public EventCostChangedEvent(Event aggregate) {
        super(aggregate);
    }
}
