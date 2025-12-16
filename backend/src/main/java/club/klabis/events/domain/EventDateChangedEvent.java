package club.klabis.events.domain;

import club.klabis.shared.domain.AggregateDomainEvent;

public class EventDateChangedEvent extends AggregateDomainEvent<Event> {
    protected EventDateChangedEvent(Event aggregate) {
        super(aggregate);
    }
}
