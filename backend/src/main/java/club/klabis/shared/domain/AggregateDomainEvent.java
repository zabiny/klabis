package club.klabis.shared.domain;

import org.springframework.data.domain.AbstractAggregateRoot;

public abstract class AggregateDomainEvent<E extends AbstractAggregateRoot<E>> extends DomainEventBase {

    private E aggregate;

    protected AggregateDomainEvent(E aggregate) {
        this.aggregate = aggregate;
    }

    // TODO: remove - there must not be full source Entity - client may use it and cause harm because there are some state data (events) which weren't processed yet and it would cycle up. There must be just ID + change data - see Finance events.
    public E getAggregate() {
        return aggregate;
    }
}
