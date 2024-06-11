package club.klabis.domain.members.events;

import org.jmolecules.ddd.types.AggregateRoot;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.ZonedDateTime;

public abstract class DomainEvent<E extends AbstractAggregateRoot<E>> {

    private ZonedDateTime createdAt;
    private E aggregate;

    protected DomainEvent(E aggregate) {
        createdAt = ZonedDateTime.now();
        this.aggregate = aggregate;
    }

    public E getAggregate() {
        return aggregate;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
}
