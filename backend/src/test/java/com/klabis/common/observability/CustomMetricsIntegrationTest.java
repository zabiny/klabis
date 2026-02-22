package com.klabis.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test verifying custom metrics are updated when Spring Modulith processes events.
 *
 * <p><b>Purpose:</b> Tests that custom Spring Modulith metrics (counter, gauge, histogram)
 * are properly updated when domain events are published and processed through the event system.
 *
 * <p><b>What It Tests:</b>
 * <ul>
 *   <li>Events published counter increments when events are published</li>
 *   <li>Event latency histogram records processing times</li>
 *   <li>Incomplete events gauge reflects actual event publication table state</li>
 * </ul>
 *
 * <p><b>Test Strategy:</b>
 * <ol>
 *   <li>Use MetricsTrackingEventHandler directly to record metrics</li>
 *   <li>Verify custom metrics reflect the expected values</li>
 * </ol>
 *
 * @see MeterRegistry
 */
@DisplayName("Framework: Spring Modulith Custom Metrics")
@ApplicationModuleTest
@ActiveProfiles("test")
class CustomMetricsIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        assertThat(meterRegistry).isNotNull();
    }

    record SuccessfulEvent() {
    }

    record FailingEvent() {

    }

    Counter getPublishedEvents() {
        return meterRegistry.find(CustomMetricsConfiguration.METRIC_NAME_LISTENERS_CALLED).counter();
    }

    Gauge getIncompleteEvents() {
        return meterRegistry.find(CustomMetricsConfiguration.METRIC_NAME_INCOMPLETE_EVENTS).gauge();
    }

    DistributionSummary getLatency() {
        return meterRegistry.find(CustomMetricsConfiguration.METRIC_NAME_LISTENERS_EXECUTION_TIME).summary();
    }

    @DisplayName("Custom klabis metrics should track published Klabis events")
    @Test
    void itShouldTrackPublishedEvents(Scenario scenario) {
        // Reset metrics before test
        double initialIncomplete = getIncompleteEvents().value();
        double initialCounter = getPublishedEvents().count();
        double initialLatency = getLatency().count();

        // klabis successful event
        scenario.publish(new SuccessfulEvent())
                .andWaitForEventOfType(SuccessfulEvent.class)
                .toArrive();

        // Wait for Spring Modulith to update completion_date
        await().atMost(2, SECONDS).untilAsserted(() ->
                assertThat(getPublishedEvents().count()).isGreaterThanOrEqualTo(initialCounter + 1)
        );

        // klabis failing event
        scenario.publish(new FailingEvent())
                .andWaitForEventOfType(FailingEvent.class)
                .toArrive();

        // non-klabis event - shouldn't be tracked
        scenario.publish(new ExitCodeEvent(this, 1))
                .andWaitForEventOfType(ExitCodeEvent.class)
                .toArrive();

        // Wait for Spring Modulith to process all events
        await().atMost(2, SECONDS).untilAsserted(() -> {
            assertThat(getPublishedEvents().count()).describedAs("Published events count")
                    .isEqualTo(initialCounter + 2);
            assertThat(getIncompleteEvents().value()).describedAs("Incomplete events count")
                    .isEqualTo(initialIncomplete + 1);
            assertThat(getLatency().count()).describedAs("Latency count").isEqualTo((long) (initialLatency + 1));
        });
    }

    @TestConfiguration
    static class TestEventsListener {

        private static final Logger LOG = LoggerFactory.getLogger(TestEventsListener.class);

        @ApplicationModuleListener
        void onSuccessEvent(SuccessfulEvent event) {
            LOG.info("Successfully processed event: {}", event);
        }

        @ApplicationModuleListener
        void onFailedEvent(FailingEvent event) {
            LOG.info("FailingEvent event: {}", event);
            throw new RuntimeException("Failing event listener for testing purpose");
        }
    }

}
