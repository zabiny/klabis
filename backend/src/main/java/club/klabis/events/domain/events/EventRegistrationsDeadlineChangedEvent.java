package club.klabis.events.domain.events;

import club.klabis.events.domain.Event;
import club.klabis.shared.domain.AggregateDomainEvent;

public class EventRegistrationsDeadlineChangedEvent extends AggregateDomainEvent<Event> {
    public EventRegistrationsDeadlineChangedEvent(Event aggregate) {
        super(aggregate);
    }
}
