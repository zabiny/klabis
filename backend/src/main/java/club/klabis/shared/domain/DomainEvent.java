package club.klabis.shared.domain;

import java.time.ZonedDateTime;

public interface DomainEvent {
    ZonedDateTime getCreatedAt();
}
