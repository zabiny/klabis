package club.klabis.events.domain;

import club.klabis.shared.domain.AggregateDomainEvent;

public class EventRegistrationsDeadlineChangedEvent extends AggregateDomainEvent<Event> {
    protected EventRegistrationsDeadlineChangedEvent(Event aggregate) {
        super(aggregate);
    }
}
