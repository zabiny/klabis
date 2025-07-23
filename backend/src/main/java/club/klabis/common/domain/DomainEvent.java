package club.klabis.common.domain;

import java.time.ZonedDateTime;

public interface DomainEvent {
    ZonedDateTime getCreatedAt();
}
