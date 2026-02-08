package com.klabis.config;

import com.klabis.KlabisApplication;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * AOP aspect that tracks metrics for Spring Modulith event listeners.
 *
 * <h2>Purpose</h2>
 * Automatically measures and records metrics for all @EventListener methods
 * (including @ApplicationModuleListener), providing observability into event processing.
 *
 * <h2>Metrics Tracked</h2>
 * <ul>
 *   <li><b>Published Events Counter ({@value CustomMetricsConfiguration#METRIC_NAME_LISTENERS_CALLED}):</b>
 *       Increments when Klabis events are processed by listeners</li>
 *   <li><b>Event Latency ({@value CustomMetricsConfiguration#METRIC_NAME_LISTENERS_EXECUTION_TIME}):</b>
 *       Records execution time of event listener methods in milliseconds</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 * <ol>
 *   <li>Intercepts all @EventListener method calls</li>
 *   <li>Filters for Klabis domain events only</li>
 *   <li>Increments published events counter</li>
 *   <li>Measures listener execution time</li>
 *   <li>Records latency metric after listener completes</li>
 * </ol>
 *
 * <p><b>Note:</b> This does NOT include the time events spend waiting in the
 * Spring Modulith outbox table. It only measures listener execution time.
 *
 * @see org.springframework.modulith.events.ApplicationModuleListener
 * @see org.springframework.context.event.EventListener
 * @see CustomMetricsConfiguration
 */
@Aspect
@Component
@ConditionalOnProperty(
        prefix = "klabis.metrics.custom",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CustomMetricsTrackingAspect {

    private static final Package KLABIS_PACKAGE = KlabisApplication.class.getPackage();
    private static final Logger log = LoggerFactory.getLogger(CustomMetricsTrackingAspect.class);

    private final MeterRegistry meterRegistry;

    public CustomMetricsTrackingAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(org.springframework.context.event.EventListener) && execution(* com.klabis..*(com.klabis..*))")
    public Object trackEventListeners(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleTracking(joinPoint);
    }

    @Around("@annotation(org.springframework.transaction.event.TransactionalEventListener) && execution(* com.klabis..*(com.klabis..*))")
    public Object trackTransactionalEventListeners(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleTracking(joinPoint);
    }

    @Around("@annotation(org.springframework.modulith.events.ApplicationModuleListener) && execution(* com.klabis..*(com.klabis..*))")
    public Object trackApplicationModuleListeners(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleTracking(joinPoint);
    }

    private Object handleTracking(ProceedingJoinPoint joinPoint) throws Throwable {
        // Extract event from method arguments (first parameter is always the event)
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return joinPoint.proceed();
        }

        Object event = args[0];

        // Measure execution time
        Instant startTime = Instant.now();

        try {
            // Execute the actual listener method
            Object result = joinPoint.proceed();

            Duration latency = Duration.between(startTime, Instant.now());
            trackEventSuccess(event, latency);

            return result;
        } catch (Throwable throwable) {
            // Still record latency even if listener fails
            Duration latency = Duration.between(startTime, Instant.now());
            trackEventFailure(event, latency);

            throw throwable;
        }
    }

    public void trackEventFailure(Object event, Duration latency) {
        if (isKlabisEvent(event)) {
            log.debug("Tracking failed processing of event {}", event.getClass().getSimpleName());
            getPublishedEventsCounter().increment();
        }

    }

    public void trackEventSuccess(Object event, Duration latency) {
        if (isKlabisEvent(event)) {
            log.debug("Tracking success processing of event {}", event.getClass().getSimpleName());
            getPublishedEventsCounter().increment();
            recordLatency(latency);
        }
    }

    private Counter getPublishedEventsCounter() {
        return meterRegistry.find(CustomMetricsConfiguration.METRIC_NAME_LISTENERS_CALLED).counter();
    }

    private DistributionSummary getEventLatencySummary() {
        return meterRegistry.find(CustomMetricsConfiguration.METRIC_NAME_LISTENERS_EXECUTION_TIME).summary();
    }

    private void recordLatency(Duration latency) {
        DistributionSummary summary = getEventLatencySummary();
        if (summary != null) {
            double millis = latency.toMillis();
            summary.record(millis);
        }
    }

    private boolean isKlabisEvent(Object event) {
        Package eventPackage = event.getClass().getPackage();
        return eventPackage != null &&
               eventPackage.getName().startsWith(KLABIS_PACKAGE.getName());
    }
}
