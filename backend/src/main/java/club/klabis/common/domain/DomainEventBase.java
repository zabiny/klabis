package club.klabis.common.domain;

import java.time.ZonedDateTime;

public abstract class DomainEventBase implements DomainEvent {

    private ZonedDateTime createdAt;

    protected DomainEventBase() {
        createdAt = ZonedDateTime.now();
    }

    @Override
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
}
