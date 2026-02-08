package com.klabis.config;

import com.klabis.members.MemberCreatedEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for event lifecycle logging in Spring Modulith.
 *
 * <h2>Purpose</h2>
 * Enables structured logging for event lifecycle stages to provide observability
 * and debugging capabilities for the event-driven architecture.
 *
 * <h2>What Gets Logged</h2>
 * When DEBUG logging is enabled, Spring Modulith logs the following event lifecycle stages:
 *
 * <h3>Example Log Output:</h3>
 * <pre>
 * 2026-01-13 10:30:45.123 DEBUG [event-publication-1] o.s.m.e.EventPublicationRegistry :
 *   Publishing event MemberCreatedEvent{eventId=123e4567, memberId=789, ...}
 *
 * 2026-01-13 10:30:45.456 DEBUG [event-publication-1] o.s.m.e.EventPublicationRegistry :
 *   Event MemberCreatedEvent completed for listener MemberCreatedEventHandler.onMemberCreated
 *
 * 2026-01-13 10:30:45.789 ERROR [event-publication-2] o.s.m.e.EventPublicationRegistry :
 *   Event processing failed for listener SomeEventHandler.onEvent: java.lang.RuntimeException: ...
 * </pre>
 *
 * <h3>Event Lifecycle Stages:</h3>
 * <ul>
 *   <li><b>Persisted:</b> Event written to outbox table (EVENT_PUBLICATION)</li>
 *   <li><b>Published:</b> Event delivered to listener</li>
 *   <li><b>Completed:</b> Event successfully processed by listener</li>
 *   <li><b>Failed:</b> Event processing threw exception</li>
 * </ul>
 *
 * <h2>Enabling/Disabling</h2>
 * Event logging can be controlled via two properties:
 * <pre>
 * # Enable/disable event logging configuration bean
 * klabis.events.logging.enabled: true  # default
 *
 * # Set the actual log level for Spring Modulith event processing
 * logging.level.org.springframework.modulith.events: DEBUG  # default
 * </pre>
 *
 * <h2>Privacy Considerations</h2>
 *
 * <p><b>CRITICAL:</b> Event lifecycle logging relies on each event's {@code toString()} method.
 * Ensure all domain event classes override {@code toString()} to exclude PII (names, emails,
 * phone numbers, addresses, dates of birth, etc.).</p>
 *
 * <p><b>What to log in toString():</b></p>
 * <ul>
 *   <li>Event type and ID (UUIDs)</li>
 *   <li>Aggregate root ID (if numeric/UUID, not identifying)</li>
 *   <li>Business identifiers (registration numbers, etc.)</li>
 *   <li>Timestamps</li>
 *   <li>Flags/statuses (isMinor, isActive, etc.)</li>
 * </ul>
 *
 * <p><b>Never log in toString():</b></p>
 * <ul>
 *   <li>Personal names (firstName, lastName)</li>
 *   <li>Contact information (email, phone, address)</li>
 *   <li>Dates of birth</li>
 *   <li>Financial information</li>
 *   <li>Any other GDPR-regulated data</li>
 * </ul>
 *
 * <p><b>Example:</b> See {@link MemberCreatedEvent#toString()}
 * for an example of PII-safe toString() implementation.</p>
 *
 * <h2>Alternative: Programmatic Event Logging</h2>
 * For custom event lifecycle logging, you can create event handlers that log at specific stages:
 * <pre>
 * {@code
 * @Component
 * public class EventAuditLogger {
 *
 *     private static final Logger auditLog = LoggerFactory.getLogger("EVENT_AUDIT");
 *
 *     @ApplicationModuleListener
 *     public void logEventProcessing(Object event) {
 *         auditLog.info("Processing event: {}", event.getClass().getSimpleName());
 *     }
 * }
 * }
 * </pre>
 *
 * @see org.springframework.modulith.events.ApplicationModuleListener
 * @see org.springframework.modulith.events.EventPublicationRegistry
 * @see <a href="https://docs.spring.io/spring-modulith/reference/events.html">Spring Modulith Event Documentation</a>
 */
@Configuration
@ConditionalOnProperty(
        prefix = "klabis.events.logging",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class EventLoggingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EventLoggingConfiguration.class);

    /**
     * Logs event logging configuration status on startup.
     * Only logs if the configuration bean is actually instantiated.
     */
    @PostConstruct
    public void logConfigurationStatus() {
        log.info("Event lifecycle logging configuration loaded. " +
                 "Spring Modulith event processing will be logged at DEBUG level. " +
                 "Set logging.level.org.springframework.modulith.events=DEBUG to see event flow.");
    }
}
