package club.klabis.domain;

import java.time.ZonedDateTime;

public interface DomainEvent {
    ZonedDateTime getCreatedAt();
}
