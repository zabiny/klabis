-- Test tables for Spring Modulith event processing tests
-- These tables are used to test event-driven behavior between Order and Payment aggregates
-- Updated to support Spring Data JDBC with optimistic locking

-- Order aggregate table
CREATE TABLE test_order
(
    id             UUID PRIMARY KEY,
    order_number   VARCHAR(50)    NOT NULL UNIQUE,
    customer_email VARCHAR(255)   NOT NULL,
    amount         DECIMAL(19, 2) NOT NULL,
    status         VARCHAR(50)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version        BIGINT                  DEFAULT 0
);

-- Payment aggregate table
CREATE TABLE test_payment
(
    id                UUID PRIMARY KEY,
    order_id          UUID           NOT NULL,
    payment_reference VARCHAR(50)    NOT NULL UNIQUE,
    amount            DECIMAL(19, 2) NOT NULL,
    status            VARCHAR(50)    NOT NULL,
    processed_at      TIMESTAMP,
    version           BIGINT DEFAULT 0,
    FOREIGN KEY (order_id) REFERENCES test_order (id)
);

-- Idempotency tracking table (for testing idempotent event handlers)
CREATE TABLE test_processed_payment_events
(
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
