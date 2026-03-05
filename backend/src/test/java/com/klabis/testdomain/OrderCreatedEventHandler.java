package com.klabis.testdomain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Event handler for OrderCreatedEvent.
 *
 * <p>This handler creates a Payment when an Order is created.
 * Used to test Spring Modulith event processing behavior.
 *
 * <p><b>Test Features:</b>
 * <ul>
 *   <li>Configurable failure simulation via {@link #setShouldFail(boolean)}</li>
 *   <li>Idempotency via both entity check and processed events table</li>
 *   <li>Async execution (via @ApplicationModuleListener)</li>
 *   <li>Separate transaction (via @Transactional(propagation = REQUIRES_NEW))</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventHandler {

    private final TestPaymentRepository paymentRepository;
    private final TestProcessedPaymentEventRepository processedEventRepository;

    private boolean shouldFail = false;
    private int executionCount = 0;

    /**
     * Handle OrderCreatedEvent by creating a Payment.
     *
     * <p>This method demonstrates:
     * <ul>
     *   <li>Idempotency via entity check (payment already exists for order)</li>
     *   <li>Idempotency via processed events table (event already processed)</li>
     *   <li>Configurable failure simulation for testing</li>
     * </ul>
     */
    @ApplicationModuleListener
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onOrderCreated(OrderCreatedEvent event) {
        executionCount++;
        log.info("Processing OrderCreatedEvent (execution #{}) for order: {}, event id: {}",
                executionCount, event.getOrderNumber(), event.getEventId());

        // Idempotency check 1: Check if payment already exists for this order
        if (paymentRepository.findByOrderId(event.getOrderId()).isPresent()) {
            log.info("Payment already exists for order {}, skipping", event.getOrderNumber());
            return;
        }

        // Idempotency check 2: Check if event was already processed
        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return;
        }

        // Simulate failure if configured (for testing error handling)
        if (shouldFail) {
            log.warn("Failure simulation enabled, throwing exception for event {}", event.getEventId());
            throw new RuntimeException("Simulated failure in OrderCreatedEventHandler");
        }

        // Create and save payment
        Payment payment = new Payment(
                UUID.randomUUID(),
                event.getOrderId(),
                "PAY-" + event.getOrderNumber(),
                event.getAmount()
        );
        payment.markAsProcessed();
        paymentRepository.save(payment);

        // Mark event as processed
        processedEventRepository.save(new ProcessedPaymentEvent(event.getEventId()));

        log.info("Payment created and event marked as processed for order: {}", event.getOrderNumber());
    }

    /**
     * Configure whether the handler should throw an exception.
     * Used to test error handling and retry behavior.
     */
    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
        log.info("Failure simulation set to: {}", shouldFail);
    }

    /**
     * Get the number of times this handler has been executed.
     * Used to verify async execution and retry behavior.
     */
    public int getExecutionCount() {
        return executionCount;
    }

    /**
     * Reset execution count.
     * Used to reset state between tests.
     */
    public void resetExecutionCount() {
        this.executionCount = 0;
    }
}
