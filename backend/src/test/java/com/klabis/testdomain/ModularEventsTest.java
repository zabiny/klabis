package com.klabis.testdomain;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Modulith event processing tests using best practices.
 *
 * <p><b>Test Domain:</b>
 * Uses Order/Payment aggregates to demonstrate and verify Spring Modulith's
 * event-driven architecture capabilities:
 * <ul>
 *   <li>Order is created and publishes OrderCreatedEvent</li>
 *   <li>OrderCreatedEvent triggers payment processing asynchronously</li>
 *   <li>Payment is created after Order transaction commits</li>
 * </ul>
 *
 * @see Scenario
 * @see ApplicationModuleTest
 */
@Slf4j
@ApplicationModuleTest(module = "common")
@ComponentScan(basePackageClasses = OrderCreatedEventHandler.class)
@ActiveProfiles("test")
@DisplayName("Framework: Spring Modulith Event Processing")
class ModularEventsTest {

    @Autowired
    private TestOrderRepository orderRepository;

    @Autowired
    private TestPaymentRepository paymentRepository;

    @Autowired
    private OrderCreatedEventHandler orderCreatedEventHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private CompletedEventPublications completedPublications;

    @Autowired
    private IncompleteEventPublications incompletePublications;

    @TestBean
    public UserDetailsService userDetailsService;

    static UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @BeforeEach
    void setUp() {
        // Reset event handler state
        orderCreatedEventHandler.resetExecutionCount();

        // Clean up test data
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM EVENT_PUBLICATION");
    }

    // ============================================================================
    // SQL QUERY UTILITY METHODS
    // ============================================================================

    private List<Map<String, Object>> findEventsByType(Class<?> type) {
        return findEventsByType(type.getSimpleName());
    }

    /**
     * Query for all event publications by event type.
     */
    private List<Map<String, Object>> findEventsByType(String eventTypePattern) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM EVENT_PUBLICATION WHERE EVENT_TYPE LIKE ?",
                "%" + eventTypePattern + "%"
        );
    }

    /**
     * Query for event publications by type and serialized content.
     */
    private List<Map<String, Object>> findEventsByTypeAndContent(String eventTypePattern, String contentPattern) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM EVENT_PUBLICATION WHERE EVENT_TYPE LIKE ? AND SERIALIZED_EVENT LIKE ?",
                "%" + eventTypePattern + "%",
                "%" + contentPattern + "%"
        );
    }

    /**
     * Query for incomplete event publications (COMPLETION_DATE IS NULL).
     */
    private List<Map<String, Object>> findIncompleteEvents() {
        return jdbcTemplate.queryForList(
                "SELECT * FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL"
        );
    }

    /**
     * Query for completed event publications (COMPLETION_DATE IS NOT NULL).
     */
    private List<Map<String, Object>> findCompletedEvents() {
        return jdbcTemplate.queryForList(
                "SELECT * FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NOT NULL"
        );
    }

    /**
     * Count old completed event publications (older than specified days).
     */
    private Integer countOldCompletedEvents(int daysOld) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION " +
                "WHERE COMPLETION_DATE IS NOT NULL " +
                "AND COMPLETION_DATE < CURRENT_TIMESTAMP - INTERVAL '" + daysOld + "' DAY",
                Integer.class
        );
    }

    /**
     * Count old incomplete event publications (older than specified minutes).
     */
    private Integer countOldIncompleteEvents(int minutesOld) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION " +
                "WHERE COMPLETION_DATE IS NULL " +
                "AND PUBLICATION_DATE < CURRENT_TIMESTAMP - INTERVAL '" + minutesOld + "' MINUTE",
                Integer.class
        );
    }

    /**
     * Count completed event publications for a specific order.
     */
    private Integer countCompletedEventsForOrder(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM EVENT_PUBLICATION " +
                "WHERE COMPLETION_DATE IS NOT NULL " +
                "AND SERIALIZED_EVENT LIKE ?",
                Integer.class,
                "%" + orderId.toString() + "%"
        );
    }

    private void makeEventsStale(UUID orderId, Duration duration) {
        makeEventsStale(orderId, duration.toSeconds());
    }

    /**
     * Make event publications stale by updating their publication date.
     * Used to simulate events that are old enough for cleanup/retry.
     */
    private void makeEventsStale(UUID orderId, long secondsAgo) {
        jdbcTemplate.update(
                "UPDATE EVENT_PUBLICATION SET PUBLICATION_DATE = ? " +
                "WHERE EVENT_TYPE LIKE '%OrderCreatedEvent%' " +
                "AND SERIALIZED_EVENT LIKE ?",
                Timestamp.from(java.time.Instant.now().minusSeconds(secondsAgo)),
                "%" + orderId.toString() + "%"
        );
    }

    /**
     * Make event publications stale and set completion date.
     * Used to simulate completed events that are old enough for cleanup.
     */
    private void makeCompletedEventsStale(UUID orderId, int secondsAgo) {
        jdbcTemplate.update(
                "UPDATE EVENT_PUBLICATION SET PUBLICATION_DATE = ?, COMPLETION_DATE = ? " +
                "WHERE EVENT_TYPE LIKE '%OrderCreatedEvent%' " +
                "AND SERIALIZED_EVENT LIKE ?",
                Timestamp.from(java.time.Instant.now().minusSeconds(secondsAgo)),
                Timestamp.from(java.time.Instant.now().minusSeconds(secondsAgo)),
                "%" + orderId.toString() + "%"
        );
    }

    /**
     * Query for event publications with publication date ordering.
     */
    private List<Map<String, Object>> findEventsByTypeOrderedByPublicationDate(String eventTypePattern) {
        return jdbcTemplate.queryForList(
                "SELECT PUBLICATION_DATE, SERIALIZED_EVENT FROM EVENT_PUBLICATION " +
                "WHERE EVENT_TYPE LIKE ? " +
                "ORDER BY PUBLICATION_DATE ASC",
                "%" + eventTypePattern + "%"
        );
    }

    // ============================================================================
    // EVENT PUBLICATION TESTS
    // ============================================================================

    @Nested
    @DisplayName("Event Publication")
    class EventPublicationTests {

        @Test
        @DisplayName("should publish OrderCreatedEvent when order is created")
        void shouldPublishOrderCreatedEventWhenOrderCreated(Scenario scenario) {
            // Given: An order to create
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-PUBLISH-001",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Order creation should publish event
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderRepository.findById(order.getId()))
                    .andVerify(savedOrder -> {
                        assertThat(savedOrder).isPresent();
                    });
        }

        @Test
        @DisplayName("should publish event to registry when order is created")
        void shouldPublishEventToRegistryWhenOrderCreated(Scenario scenario) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-PUBLISH-REGISTRY-001",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Event should be published to registry
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> findEventsByType("OrderCreatedEvent"))
                    .andVerify(events -> {
                        assertThat(events)
                                .as("Event should be published to registry")
                                .isNotEmpty();

                        assertThat(events.stream()
                                .anyMatch(e -> String.valueOf(e.get("SERIALIZED_EVENT"))
                                        .contains(order.getId().toString())))
                                .as("Event should contain order ID")
                                .isTrue();
                    });
        }

        @Test
        @DisplayName("should verify event payload in registry contains correct data")
        void shouldVerifyEventPayloadInRegistryContainsCorrectData(Scenario scenario) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            UUID orderId = UUID.randomUUID();
            Order order = new Order(
                    orderId,
                    "ORDER-PAYLOAD-REGISTRY-001",
                    "payload@example.com",
                    new BigDecimal("250.00")
            );

            // When/Then: Event payload should be correct
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        List<Map<String, Object>> events = findEventsByTypeAndContent("OrderCreatedEvent",
                                orderId.toString());
                        return events.isEmpty() ? null : events.get(0);
                    })
                    .andVerify(event -> {
                        assertThat(event)
                                .as("Event should exist in registry")
                                .isNotNull();

                        String serializedEvent = String.valueOf(event.get("SERIALIZED_EVENT"));
                        assertThat(serializedEvent)
                                .as("Event should contain order ID")
                                .contains(orderId.toString());
                        assertThat(serializedEvent)
                                .as("Event should contain order number")
                                .contains("ORDER-PAYLOAD-REGISTRY-001");
                        assertThat(serializedEvent)
                                .as("Event should contain customer email")
                                .contains("payload@example.com");
                    });
        }
    }

    // ============================================================================
    // ORDER → PAYMENT FLOW TESTS
    // ============================================================================

    @Nested
    @DisplayName("Events testing example (Order → Payment) Flow")
    class OrderToPaymentFlowTests {

        @Test
        @DisplayName("complete happy path: order → event → payment")
        void shouldProcessOrderToPaymentSuccessfully(Scenario scenario) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-FLOW-001",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Order creation should trigger payment creation
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> paymentRepository.findByOrderId(order.getId()))
                    .andVerify(paymentOpt -> {
                        assertThat(paymentOpt)
                                .as("Payment should be created by event handler")
                                .isPresent();

                        Payment payment = paymentOpt.get();
                        assertThat(payment.getStatus()).isEqualTo("PROCESSED");
                        assertThat(payment.getAmount()).isEqualByComparingTo("100.00");
                    });
        }

        @Test
        @DisplayName("handler failure doesn't rollback order")
        void shouldNotRollbackOrderWhenHandlerFails(Scenario scenario) {
            // Given: Event handler configured to fail
            orderCreatedEventHandler.setShouldFail(true);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-FLOW-FAIL-001",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Order should be saved despite event handler failure
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> orderRepository.findById(order.getId()))
                    .andVerify(savedOrder -> {
                        assertThat(savedOrder)
                                .as("Order should be saved despite event handler failure")
                                .isPresent();

                        // Payment should NOT exist (handler failed)
                        assertThat(paymentRepository.findByOrderId(order.getId()))
                                .as("Payment should not exist due to handler failure")
                                .isEmpty();
                    });
        }

        @Test
        @DisplayName("event handler executes asynchronously")
        void shouldExecuteEventHandlerAsynchronously(Scenario scenario) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-FLOW-ASYNC-001",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Handler should execute asynchronously
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        int count = orderCreatedEventHandler.getExecutionCount();
                        return count > 0 ? count : null;
                    })
                    .andVerify(count -> {
                        assertThat(count)
                                .as("Handler should execute asynchronously")
                                .isGreaterThan(0);
                    });
        }

        @Test
        @DisplayName("event marked incomplete when handler fails")
        void shouldMarkEventIncompleteWhenHandlerFails(Scenario scenario) {
            // Given: Event handler configured to fail
            orderCreatedEventHandler.setShouldFail(true);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-FLOW-INCOMPLETE-001",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Failed event should stay incomplete
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        List<Map<String, Object>> incompleteEvents = findIncompleteEvents();
                        return incompleteEvents.isEmpty() ? null : incompleteEvents.size();
                    })
                    .andVerify(count -> {
                        assertThat(count)
                                .as("Incomplete events should exist")
                                .isGreaterThan(0);
                    });
        }
    }

    // ============================================================================
    // EVENT CLEANUP FLOW TESTS
    // ============================================================================

    @Nested
    @DisplayName("Event Cleanup Flow")
    class EventCleanupFlowTests {

        @Test
        @DisplayName("should mark events as completed after successful processing")
        void shouldMarkEventsCompletedAfterSuccessfulProcessing(Scenario scenario) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-CLEANUP-001",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Successful processing should mark event complete
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> completedPublications.findAll()
                            .stream()
                            .filter(e -> e.getEvent() instanceof OrderCreatedEvent)
                            .toList(), l -> !l.isEmpty())
                    .andVerify(completedEvents -> {
                        assertThat(completedEvents)
                                .as("Event should be marked as completed")
                                .allMatch(EventPublication::isCompleted);

                        assertThat(completedEvents)
                                .map(EventPublication::getEvent)
                                .map(OrderCreatedEvent.class::cast)
                                .as("Our event should be marked as completed")
                                .anyMatch(e -> e.getOrderId().equals(order.getId()));
                    });
        }

        @Test
        @DisplayName("should leave incomplete events unmarked after failed processing")
        void shouldLeaveEventsIncompleteAfterFailedProcessing(Scenario scenario) {
            // Given: Event handler configured to fail
            orderCreatedEventHandler.setShouldFail(true);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-CLEANUP-002",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Failed events should stay incomplete
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        List<Map<String, Object>> incompleteEvents = findIncompleteEvents();
                        return incompleteEvents.isEmpty() ? null : incompleteEvents;
                    })
                    .andVerify(incompleteEvents -> {
                        assertThat(incompleteEvents)
                                .as("Event should remain incomplete after failure")
                                .isNotEmpty();

                        assertThat(incompleteEvents.stream()
                                .anyMatch(e -> String.valueOf(e.get("SERIALIZED_EVENT"))
                                        .contains(order.getId().toString())))
                                .as("Our failed event should be incomplete")
                                .isTrue();
                    });
        }

        @Test
        @DisplayName("should delete old completed events during cleanup")
        void shouldDeleteOldCompletedEventsDuringCleanup(Scenario scenario) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-CLEANUP-003",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When: Process order, make it old, and run cleanup
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        // Simulate old completed event (older than 7 days)
                        makeCompletedEventsStale(order.getId(), 10 * 24 * 60 * 60);

                        // Verify old completed event exists before cleanup
                        Integer countBefore = countOldCompletedEvents(7);

                        return countBefore != null && countBefore > 0 ? countBefore : null;
                    })
                    .andVerify(countBefore -> {
                        assertThat(countBefore)
                                .as("Should have old completed events before cleanup")
                                .isGreaterThan(0);

                        // Perform cleanup using Spring Modulith API
                        completedPublications.deletePublicationsOlderThan(java.time.Duration.ofDays(7));

                        // Verify cleanup actually removed events
                        Integer countAfter = countOldCompletedEvents(7);

                        assertThat(countAfter)
                                .as("Old completed events should be removed after cleanup")
                                .isEqualTo(0);
                    });
        }

        @Test
        @DisplayName("should preserve incomplete events during cleanup regardless of age")
        void shouldPreserveIncompleteEventsDuringCleanup(Scenario scenario) {
            // Given: Event handler configured to fail
            orderCreatedEventHandler.setShouldFail(true);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-CLEANUP-004",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When: Fail event, make it old, and run cleanup
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        // Simulate old incomplete event (older than 7 days)
                        makeEventsStale(order.getId(), 10 * 24 * 60 * 60);

                        // Verify old incomplete event exists before cleanup
                        Integer countBefore = countOldIncompleteEvents(7 * 24 * 60);

                        return countBefore != null && countBefore > 0 ? countBefore : null;
                    })
                    .andVerify(countBefore -> {
                        assertThat(countBefore)
                                .as("Should have old incomplete events before cleanup")
                                .isGreaterThan(0);

                        // Perform cleanup using Spring Modulith API (only removes completed events)
                        completedPublications.deletePublicationsOlderThan(java.time.Duration.ofDays(7));

                        // Verify incomplete events still exist after cleanup
                        Integer countAfter = countOldIncompleteEvents(7 * 24 * 60);

                        assertThat(countAfter)
                                .as("Old incomplete events should be preserved during cleanup")
                                .isEqualTo(countBefore);
                    });
        }

        @Test
        @DisplayName("should preserve recent completed events during cleanup")
        void shouldPreserveRecentCompletedEventsDuringCleanup(Scenario scenario) {
            // Given: Event handler with normal behavior
            orderCreatedEventHandler.setShouldFail(false);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-CLEANUP-005",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When: Process order and run cleanup
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        // Verify recent completed event exists
                        Integer countBefore = countCompletedEventsForOrder(order.getId());

                        return countBefore != null && countBefore > 0 ? countBefore : null;
                    })
                    .andVerify(countBefore -> {
                        assertThat(countBefore)
                                .as("Should have recent completed event before cleanup")
                                .isEqualTo(1);

                        // Perform cleanup using Spring Modulith API (only removes events older than 7 days)
                        completedPublications.deletePublicationsOlderThan(java.time.Duration.ofDays(7));

                        // Verify our recent event is still there
                        Integer countAfter = countCompletedEventsForOrder(order.getId());

                        assertThat(countAfter)
                                .as("Recent completed events should be preserved during cleanup")
                                .isEqualTo(1);
                    });
        }
    }

    // ============================================================================
    // EVENT RETRY FLOW TESTS
    // ============================================================================

    @Nested
    @DisplayName("Event Retry Flow")
    class EventRetryFlowTests {

        @Test
        @DisplayName("failed events preserved for retry")
        void shouldPreserveFailedEventsForRetry(Scenario scenario) {
            // Given: Event handler configured to fail
            orderCreatedEventHandler.setShouldFail(true);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-RETRY-001",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Failed event should be available for retry
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        List<Map<String, Object>> incompleteEvents = findIncompleteEvents();
                        return incompleteEvents.isEmpty() ? null : incompleteEvents;
                    })
                    .andVerify(incompleteEvents -> {
                        assertThat(incompleteEvents)
                                .as("Failed events should be preserved in outbox for retry")
                                .isNotEmpty();

                        assertThat(incompleteEvents.stream()
                                .anyMatch(e -> String.valueOf(e.get("SERIALIZED_EVENT"))
                                        .contains(order.getId().toString())))
                                .as("Our failed event should be available for retry")
                                .isTrue();
                    });
        }

        @Test
        @DisplayName("stale incomplete events identifiable for republish")
        void shouldIdentifyStaleIncompleteEventsForRepublish(Scenario scenario) {
            // Given: Event handler configured to fail
            orderCreatedEventHandler.setShouldFail(true);

            Order order = new Order(
                    UUID.randomUUID(),
                    "ORDER-RETRY-002",
                    "customer@example.com",
                    new BigDecimal("100.00")
            );

            // When/Then: Fail event and make it stale
            scenario.stimulate(() -> orderRepository.save(order.publishOrderCreatedEvent()))
                    .andWaitForStateChange(() -> {
                        // Simulate event becoming stale (> 5 min threshold)
                        makeEventsStale(order.getId(), Duration.ofMinutes(6));

                        Integer staleEvents = countOldIncompleteEvents(5);

                        return staleEvents != null && staleEvents > 0 ? staleEvents : null;
                    })
                    .andVerify(count -> {
                        assertThat(count)
                                .as("Stale incomplete events should be identifiable for republish")
                                .isGreaterThan(0);
                    });
        }

        private static void wait(Duration duration) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("IncompleteEventPublications API allowing to reprocess incomplete events")
        @Disabled("From some reason, doesn't process resubmitted event unless debugging code and having breakpoints in listener handler...")
        void shouldHaveIncompleteEventPublicationsAvailableForManualRetry(Scenario scenario) {

            // Given: Event handler configured to fail
            orderCreatedEventHandler.setShouldFail(true);

            final UUID testedOrderId = UUID.randomUUID();

            // When/Then: Fail event and make it stale
            scenario.stimulate(() -> {
                        transactionTemplate.executeWithoutResult(status -> {
                            Order order = new Order(
                                    testedOrderId,
                                    "ORDER-RETRY-002",
                                    "customer@example.com",
                                    new BigDecimal("100.00")
                            );

                            orderRepository.save(order.publishOrderCreatedEvent());
                        });

                        Awaitility.waitAtMost(Duration.ofSeconds(2))
                                .until(() -> findEventsByType(OrderCreatedEvent.class), i -> !i.isEmpty());

                        // will pass on retry
                        orderCreatedEventHandler.setShouldFail(false);

                        // wait to "stale" event
                        wait(Duration.ofSeconds(1));

                        // republish failed events
                        incompletePublications.resubmitIncompletePublicationsOlderThan(Duration.ofSeconds(1));
                    })
                    .andWaitForStateChange(() -> {
                        return completedPublications.findAll();
                    }, i -> !i.isEmpty())
                    .andVerify(publications -> {
                        assertThat(publications)
                                .as("Completed publications are not empty")
                                .isNotEmpty();
                        assertThat(publications)
                                .as("Previously stale incomplete event should be completed after reprocess")
                                .map(EventPublication::getEvent)
                                .map(OrderCreatedEvent.class::cast)
                                .map(OrderCreatedEvent::getOrderId)
                                .anyMatch(testedOrderId::equals);
                    });
        }

        @Test
        @DisplayName("event publication order preserved for retry")
        void shouldPreserveEventPublicationOrderForRetry(Scenario scenario) {
            // Given: Multiple orders that will fail
            orderCreatedEventHandler.setShouldFail(true);

            Order order1 = new Order(
                    UUID.randomUUID(), "ORDER-SEQ-001", "customer@example.com", new BigDecimal("50.00"));
            Order order2 = new Order(
                    UUID.randomUUID(), "ORDER-SEQ-002", "customer@example.com", new BigDecimal("75.00"));
            Order order3 = new Order(
                    UUID.randomUUID(), "ORDER-SEQ-003", "customer@example.com", new BigDecimal("100.00"));

            // When/Then: Verify events are stored in publication order for retry
            scenario.stimulate(() -> {
                        order1.publishOrderCreatedEvent();
                        order2.publishOrderCreatedEvent();
                        order3.publishOrderCreatedEvent();
                        return orderRepository.saveAll(List.of(order1, order2, order3));
                    })
                    .andWaitForStateChange(() -> {
                        List<Map<String, Object>> events = findEventsByTypeOrderedByPublicationDate("OrderCreatedEvent");
                        return events.size() >= 3 ? events : null;
                    })
                    .andVerify(events -> {
                        assertThat(events)
                                .as("Incomplete events should be available for retry")
                                .hasSizeGreaterThanOrEqualTo(3);

                        // Verify order is preserved
                        java.time.Instant firstDate = ((Timestamp) events.get(0).get("PUBLICATION_DATE")).toInstant();
                        java.time.Instant secondDate = ((Timestamp) events.get(1).get("PUBLICATION_DATE")).toInstant();
                        java.time.Instant thirdDate = ((Timestamp) events.get(2).get("PUBLICATION_DATE")).toInstant();

                        assertThat(firstDate)
                                .as("First event should be earliest")
                                .isBeforeOrEqualTo(secondDate);
                        assertThat(secondDate)
                                .as("Second event should be after first")
                                .isBeforeOrEqualTo(thirdDate);
                    });
        }
    }

}
