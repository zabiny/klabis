package com.klabis.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * Configuration for custom Spring Modulith metrics.
 *
 * <h2>Purpose</h2>
 * Exposes custom Micrometer metrics for monitoring Spring Modulith event processing.
 * These metrics integrate with Spring Boot Actuator and Prometheus for operations dashboards.
 *
 * <h2>Metrics Exposed</h2>
 * <ul>
 *   <li><b>spring.modulith.events.published</b> - Counter tracking total events published</li>
 *   <li><b>spring.modulith.events.incomplete</b> - Gauge showing current incomplete events backlog</li>
 *   <li><b>spring.modulith.events.latency</b> - Histogram of event processing time (milliseconds)</li>
 * </ul>
 *
 * <h2>Configuration</h2>
 * Custom metrics are activated via the {@code metrics} Spring profile.
 *
 * @see io.micrometer.core.instrument.MeterRegistry
 */
@Configuration
@Profile("metrics")
public class CustomMetricsConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CustomMetricsConfiguration.class);

    public static final String METRIC_NAME_INCOMPLETE_EVENTS = "klabis.events.incomplete";
    public static final String METRIC_NAME_LISTENERS_CALLED = "klabis.listeners.called";
    public static final String METRIC_NAME_LISTENERS_EXECUTION_TIME = "klabis.listeners.executionTime";

    @Bean
    MeterBinder registerMetrics(JdbcTemplate jdbcTemplate) {
        return registry -> {
            log.info("Binding Spring Modulith custom metrics to MeterRegistry %s".formatted(System.identityHashCode(
                    registry)));

            final Tag applicationTag = Tag.of("application", "klabis");

            // Counter for total events published
            Counter.builder(METRIC_NAME_LISTENERS_CALLED)
                    .description("Total number of Spring Modulith event listeners called")
                    .tags(List.of(applicationTag))
                    .register(registry);

            // Gauge for incomplete events backlog
            Gauge.builder(METRIC_NAME_INCOMPLETE_EVENTS,
                            jdbcTemplate,
                            CustomMetricsConfiguration::countIncompleteEvents)
                    .description("Current number of incomplete Spring Modulith events")
                    .tags(List.of(applicationTag))
                    .register(registry);

            // Histogram for event processing latency
            DistributionSummary.builder(METRIC_NAME_LISTENERS_EXECUTION_TIME)
                    .description("Spring Modulith event listener execution time in milliseconds")
                    .tags(List.of(applicationTag))
                    .baseUnit("milliseconds")
                    .serviceLevelObjectives()
                    .register(registry);

            log.info("Spring Modulith custom metrics registered: %s, %s, %s".formatted(
                    METRIC_NAME_LISTENERS_EXECUTION_TIME, METRIC_NAME_INCOMPLETE_EVENTS, METRIC_NAME_LISTENERS_CALLED
            ));
        };
    }

    private static double countIncompleteEvents(JdbcTemplate jdbcTemplate) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL AND EVENT_TYPE LIKE 'com.klabis.%'",
                    Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Failed to query incomplete events count: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Logs custom metrics configuration status on startup.
     */
    @PostConstruct
    public void logConfigurationStatus() {
        log.info("Custom Spring Modulith metrics enabled. " +
                 "Metrics will be available at /actuator/prometheus and /actuator/metrics");
    }

}
