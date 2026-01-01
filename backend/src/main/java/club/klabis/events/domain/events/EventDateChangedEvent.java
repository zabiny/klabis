package club.klabis.events.domain.events;

import club.klabis.events.domain.Event;
import club.klabis.shared.domain.AggregateDomainEvent;

public class EventDateChangedEvent extends AggregateDomainEvent<Event> {
    public EventDateChangedEvent(Event aggregate) {
        super(aggregate);
    }
}
