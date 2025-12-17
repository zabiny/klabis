package club.klabis.events.domain;

import club.klabis.shared.domain.AggregateDomainEvent;

public class EventCostChangedEvent extends AggregateDomainEvent<Event> {
    protected EventCostChangedEvent(Event aggregate) {
        super(aggregate);
    }
}
