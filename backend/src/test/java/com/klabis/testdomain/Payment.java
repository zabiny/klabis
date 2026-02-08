package com.klabis.testdomain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment aggregate root for testing event-driven architecture.
 *
 * <p>This entity is created in response to OrderCreatedEvent.
 * Used to verify that payment processing is triggered by events.
 */
@Table("test_payment")
public class Payment {

    @Id
    @Column("id")
    private UUID id;

    @Column("order_id")
    private UUID orderId;

    @Column("payment_reference")
    private String paymentReference;

    @Column("amount")
    private BigDecimal amount;

    @Column("status")
    private String status;

    @Column("processed_at")
    private Instant processedAt;

    @Version
    @Column("version")
    private Long version;

    public Payment(UUID id, UUID orderId, String paymentReference, BigDecimal amount) {
        this.id = id;
        this.orderId = orderId;
        this.paymentReference = paymentReference;
        this.amount = amount;
        this.status = "PENDING";
    }

    // Default constructor required by Spring Data JDBC
    protected Payment() {
        // Fields will be set by Spring Data JDBC via reflection
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void markAsProcessed() {
        this.status = "PROCESSED";
        this.processedAt = Instant.now();
    }

    public void markAsFailed() {
        this.status = "FAILED";
    }
}
