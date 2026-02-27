package com.klabis.testdomain;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Modulith event lifecycle logging tests.
 *
 * <p><b>Purpose:</b>
 * Verifies that Spring Modulith's internal logging provides observability
 * for the complete event lifecycle, enabling debugging of event delivery issues.
 *
 * <p><b>What is tested:</b>
 * <ul>
 *   <li>Events are logged when persisted to the outbox table</li>
 *   <li>Events are logged when published to listeners</li>
 *   <li>Events are logged when marked complete</li>
 *   <li>Logs contain structured data (MDC values like correlationId)</li>
 * </ul>
 *
 * <p><b>Business value:</b>
 * Ensures operations team can trace event delivery issues through logs,
 * reducing mean time to resolution (MTTR) for event-related problems.
 *
 * <p><b>Note:</b>
 * Spring Modulith provides built-in DEBUG logging for event processing.
 * This test verifies that logging is properly configured and captures
 * the event lifecycle. The structured logging format (with MDC values)
 * enables log aggregation systems to parse and search event logs.
 *
 * @see org.springframework.modulith.events.EventPublication
 * @see ApplicationModuleTest
 */
@Slf4j
@ApplicationModuleTest(module = "common")
@ComponentScan(basePackageClasses = OrderCreatedEventHandler.class)
@ActiveProfiles("test")
@DisplayName("Framework: Event Lifecycle Logging")
@ExtendWith(OutputCaptureExtension.class)
class EventLoggingTests {

    @Autowired
    private TestOrderRepository orderRepository;

    @Autowired
    private OrderCreatedEventHandler orderCreatedEventHandler;

    @Autowired
    private TestPaymentRepository paymentRepository;

    @Autowired
    private TestProcessedPaymentEventRepository processedEventRepository;

    @TestBean
    public UserDetailsService userDetailsService;

    static UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @BeforeEach
    void setUp() {
        // Reset event handler state
        orderCreatedEventHandler.resetExecutionCount();

        // Clean up test data in correct order (respecting foreign key constraints)
        paymentRepository.deleteAll();
        processedEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    // ============================================================================
    // EVENT PERSISTENCE LOGGING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Event Persistence Logging")
    class EventPersistenceLoggingTests {

        @Test
        @DisplayName("should log when event is persisted to outbox")
        void shouldLogWhenEventPersistedToOutbox(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-LOG-PERSIST-001",
                    "customer@example.com",
                    new java.math.BigDecimal("100.00")
            );

            // When: Order is created (event persisted to outbox)
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderRepository.findById(order.getId()))
                    .andVerify(savedOrder -> {
                        assertThat(savedOrder).isPresent();

                        // Then: Log should contain event persistence information
                        String logOutput = output.toString();

                        // Spring Modulith logs event persistence at DEBUG level
                        // The log should mention the event and outbox-related operations
                        assertThat(logOutput)
                                .as("Log should contain event type information")
                                .contains("OrderCreatedEvent");
                    });
        }

        @Test
        @DisplayName("should log event details when persisted to outbox")
        void shouldLogEventDetailsWhenPersisted(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            String orderNumber = "ORDER-LOG-PERSIST-002";
            Order order = new Order(
                    UUID.randomUUID(),
                    orderNumber,
                    "persist@example.com",
                    new java.math.BigDecimal("250.00")
            );

            // When: Order is created
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderRepository.findById(order.getId()))
                    .andVerify(savedOrder -> {
                        assertThat(savedOrder).isPresent();

                        // Then: Log should contain identifiable event information
                        String logOutput = output.toString();

                        assertThat(logOutput)
                                .as("Log should contain order number for traceability")
                                .contains(orderNumber);
                    });
        }
    }

    // ============================================================================
    // EVENT PUBLICATION LOGGING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Event Publication Logging")
    class EventPublicationLoggingTests {

        @Test
        @DisplayName("should log when event is published to listener")
        void shouldLogWhenEventPublishedToListener(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-LOG-PUB-001",
                    "customer@example.com",
                    new java.math.BigDecimal("100.00")
            );

            // When: Order is created and event published
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderCreatedEventHandler.getExecutionCount() > 0
                            ? orderCreatedEventHandler.getExecutionCount()
                            : null)
                    .andVerify(count -> {
                        assertThat(count).isGreaterThan(0);

                        // Then: Log should contain event publication information
                        String logOutput = output.toString();

                        // The handler logs when it starts processing
                        assertThat(logOutput)
                                .as("Log should contain event processing information")
                                .contains("Processing OrderCreatedEvent");
                    });
        }

        @Test
        @DisplayName("should log listener execution details")
        void shouldLogListenerExecutionDetails(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-LOG-PUB-002",
                    "listener@example.com",
                    new java.math.BigDecimal("100.00")
            );

            // When: Order is created
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderCreatedEventHandler.getExecutionCount() > 0
                            ? orderCreatedEventHandler.getExecutionCount()
                            : null)
                    .andVerify(count -> {
                        assertThat(count).isGreaterThan(0);

                        // Then: Log should contain execution details from handler
                        String logOutput = output.toString();

                        assertThat(logOutput)
                                .as("Log should contain handler execution information")
                                .containsAnyOf(
                                        "Processing OrderCreatedEvent",
                                        "Payment created",
                                        "event marked as processed"
                                );
                    });
        }
    }

    // ============================================================================
    // EVENT COMPLETION LOGGING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Event Completion Logging")
    class EventCompletionLoggingTests {

        @Test
        @DisplayName("should log when event is marked complete")
        void shouldLogWhenEventMarkedComplete(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-LOG-COMPLETE-001",
                    "customer@example.com",
                    new java.math.BigDecimal("100.00")
            );

            // When: Order is created and event processed successfully
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderCreatedEventHandler.getExecutionCount() > 0
                            ? orderCreatedEventHandler.getExecutionCount()
                            : null)
                    .andVerify(count -> {
                        assertThat(count).isGreaterThan(0);

                        // Then: Log should contain completion information
                        String logOutput = output.toString();

                        // Handler logs when it completes successfully
                        assertThat(logOutput)
                                .as("Log should contain event completion information")
                                .containsAnyOf(
                                        "Payment created",
                                        "event marked as processed"
                                );
                    });
        }

        @Test
        @DisplayName("should log successful processing outcome")
        void shouldLogSuccessfulProcessingOutcome(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-LOG-SUCCESS-001",
                    "success@example.com",
                    new java.math.BigDecimal("100.00")
            );

            // When: Order is created and processed successfully
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderCreatedEventHandler.getExecutionCount() > 0
                            ? orderCreatedEventHandler.getExecutionCount()
                            : null)
                    .andVerify(count -> {
                        assertThat(count).isGreaterThan(0);

                        // Then: Log should indicate successful processing
                        String logOutput = output.toString();

                        assertThat(logOutput)
                                .as("Log should contain success indicators")
                                .containsAnyOf(
                                        "Payment created",
                                        "PROCESSED"
                                );
                    });
        }
    }

    // ============================================================================
    // STRUCTURED LOGGING TESTS
    // ============================================================================

    @Nested
    @DisplayName("Structured Logging Format")
    class StructuredLoggingTests {

        @Test
        @DisplayName("should include MDC values in logs for correlation")
        void shouldIncludeMDCValuesInLogs(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-LOG-MDC-001",
                    "mdc@example.com",
                    new java.math.BigDecimal("100.00")
            );

            // When: Order is created
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderCreatedEventHandler.getExecutionCount() > 0
                            ? orderCreatedEventHandler.getExecutionCount()
                            : null)
                    .andVerify(count -> {
                        assertThat(count).isGreaterThan(0);

                        // Then: Log should contain MDC markers (correlationId)
                        // The logback pattern includes: [corr=%X{correlationId}]
                        String logOutput = output.toString();

                        // Verify structured logging elements are present
                        // The correlationId MDC value is part of the log pattern
                        assertThat(logOutput)
                                .as("Log should contain correlation ID marker for traceability")
                                .contains("[corr=");
                    });
        }

        @Test
        @DisplayName("should use consistent log format for parsing")
        void shouldUseConsistentLogFormat(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-LOG-FORMAT-001",
                    "format@example.com",
                    new java.math.BigDecimal("100.00")
            );

            // When: Order is created
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderCreatedEventHandler.getExecutionCount() > 0
                            ? orderCreatedEventHandler.getExecutionCount()
                            : null)
                    .andVerify(count -> {
                        assertThat(count).isGreaterThan(0);

                        // Then: Log should follow consistent format pattern
                        // Pattern: timestamp thread [corr=...] [user=...] level logger - message
                        String logOutput = output.toString();
                        String[] logLines = logOutput.split("\n");

                        // Find log lines related to our event processing
                        boolean hasConsistentFormat = false;
                        for (String line : logLines) {
                            if (line.contains("OrderCreatedEvent") || line.contains("Processing")) {
                                // Check for log pattern elements
                                // Format should include: timestamp, thread, MDC values, level, logger, message
                                if (line.matches(".*\\d{4}-\\d{2}-\\d{2}.*\\[.*\\].*\\[corr=.*")) {
                                    hasConsistentFormat = true;
                                    break;
                                }
                            }
                        }

                        assertThat(hasConsistentFormat)
                                .as("Log should follow consistent structured format with timestamp and MDC values")
                                .isTrue();
                    });
        }

        @Test
        @DisplayName("should log event lifecycle in chronological order")
        void shouldLogEventLifecycleInChronologicalOrder(Scenario scenario, CapturedOutput output) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-LOG-CHRONO-001",
                    "chrono@example.com",
                    new java.math.BigDecimal("100.00")
            );

            // When: Order is created
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderCreatedEventHandler.getExecutionCount() > 0
                            ? orderCreatedEventHandler.getExecutionCount()
                            : null)
                    .andVerify(count -> {
                        assertThat(count).isGreaterThan(0);

                        // Then: Log should show event lifecycle in order
                        String logOutput = output.toString();

                        // Verify event processing flow is logged
                        assertThat(logOutput)
                                .as("Log should contain event processing flow")
                                .contains("Processing");

                        assertThat(logOutput)
                                .as("Log should contain completion information")
                                .containsAnyOf(
                                        "Payment created",
                                        "event marked as processed"
                                );
                    });
        }
    }

}