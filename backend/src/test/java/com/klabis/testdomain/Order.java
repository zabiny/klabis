package com.klabis.testdomain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Order aggregate root for testing event-driven architecture.
 *
 * <p>This is a test domain model used to verify Spring Modulith event processing:
 * <ul>
 *   <li>Event publication after transaction commit</li>
 *   <li>Async event listener execution</li>
 *   <li>Error handling and retry behavior</li>
 *   <li>Event cleanup and republishing</li>
 *   <li>Idempotent event handling</li>
 * </ul>
 */
@Table("test_order")
public class Order extends AbstractAggregateRoot<Order> {

    @Id
    @Column("id")
    private UUID id;

    @Column("order_number")
    private String orderNumber;

    @Column("customer_email")
    private String customerEmail;

    @Column("amount")
    private BigDecimal amount;

    @Column("status")
    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Version
    @Column("version")
    private Long version;

    public Order(UUID id, String orderNumber, String customerEmail, BigDecimal amount) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    // Default constructor required by Spring Data JDBC
    protected Order() {
        // Fields will be set by Spring Data JDBC via reflection
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public void markAsCompleted() {
        this.status = "COMPLETED";
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Publish OrderCreatedEvent to trigger payment processing.
     * Returns this Order for fluent API chaining.
     */
    public Order publishOrderCreatedEvent() {
        registerEvent(new OrderCreatedEvent(this, this.id, this.orderNumber, this.customerEmail, this.amount));
        return this;
    }
}
