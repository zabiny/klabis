package com.klabis.testdomain;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event published when an Order is created.
 *
 * <p>This event triggers payment processing in a separate bounded context.
 * Used to test event-driven communication between Order and Payment modules.
 */
public class OrderCreatedEvent extends ApplicationEvent {

    private final UUID orderId;
    private final String orderNumber;
    private final String customerEmail;
    private final BigDecimal amount;
    private final UUID eventId;

    public OrderCreatedEvent(Object source, UUID orderId, String orderNumber, String customerEmail, BigDecimal amount) {
        super(source);
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
        this.amount = amount;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getOrderId() {
        return orderId;
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
}
