package com.klabis.testdomain;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track processed payment events for idempotency testing.
 *
 * <p>This demonstrates the database table pattern for idempotent event handling.
 * Each event ID is recorded when processed, allowing duplicate detection.
 */
@Table("test_processed_payment_events")
@Getter
public class ProcessedPaymentEvent {

    @Id
    @Column("event_id")
    private final UUID eventId;

    @Column("processed_at")
    private final Instant processedAt;

    public ProcessedPaymentEvent(UUID eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    // Default constructor required by Spring Data JDBC
    protected ProcessedPaymentEvent() {
        this.eventId = null;
        this.processedAt = null;
    }
}
