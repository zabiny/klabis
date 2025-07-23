package club.klabis.common.domain;

import org.springframework.data.domain.AbstractAggregateRoot;

public abstract class AggregateDomainEvent<E extends AbstractAggregateRoot<E>> extends DomainEventBase {

    private E aggregate;

    protected AggregateDomainEvent(E aggregate) {
        this.aggregate = aggregate;
    }

    public E getAggregate() {
        return aggregate;
    }
}
