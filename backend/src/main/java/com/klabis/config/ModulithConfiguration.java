package com.klabis.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring Modulith integration.
 *
 * <h2>Purpose</h2>
 * This configuration class documents the Spring Modulith setup for the Klabis application.
 * Spring Modulith provides a transactional outbox pattern for reliable event publishing,
 * ensuring guaranteed at-least-once delivery of domain events between modules.
 *
 * <h2>Event Publication Strategy</h2>
 *
 * <h3>Why Transactional Outbox?</h3>
 * <ul>
 *   <li><b>Dual-write problem:</b> Traditional event publishing risks event loss if the
 *       transaction commits but event processing fails, or events are published but
 *       transaction commit fails.</li>
 *   <li><b>Guaranteed delivery:</b> Outbox pattern persists events in the same transaction
 *       as the aggregate, ensuring atomicity.</li>
 *   <li><b>Automatic retry:</b> Failed event listeners are automatically retried.</li>
 *   <li><b>Observability:</b> All events are visible in the {@code event_publication} table
 *       for debugging and monitoring.</li>
 * </ul>
 *
 * <h3>How It Works</h3>
 * <ol>
 *   <li>Aggregate (e.g., {@code Member}) registers domain event ({@code MemberCreatedEvent})</li>
 *   <li>Spring Data JPA persists aggregate to database</li>
 *   <li>Spring Modulith intercepts {@code @DomainEvents} annotation and persists event
 *       to {@code event_publication} table in the same transaction</li>
 *   <li>Background thread polls outbox table for unprocessed events</li>
 *   <li>Events are published to listeners asynchronously after transaction commits</li>
 *   <li>Listeners process events (e.g., send password setup email)</li>
 *   <li>Events are marked complete (completion_date set) or republished on failure</li>
 * </ol>
 *
 * <h2>Event Listener Annotation</h2>
 *
 * <h3>Why {@code @ApplicationModuleListener}?</h3>
 * <p>
 * We use {@code @ApplicationModuleListener} (from Spring Modulith) instead of
 * {@code @TransactionalEventListener} (from Spring Framework) for event handlers because:
 *
 * <ul>
 *   <li><b>Semantically clearer:</b> Explicitly indicates inter-module communication</li>
 *   <li><b>Easier to understand:</b> Developers without deep Spring knowledge grasp the intent</li>
 *   <li><b>Built-in best practices:</b> Combines {@code @Async} + {@code @Transactional(propagation = REQUIRES_NEW)}
 *       + {@code @TransactionalEventListener(AFTER_COMMIT)} in one annotation</li>
 *   <li><b>Consistent with Modulith:</b> Standard pattern for Spring Modulith applications</li>
 * </ul>
 *
 * <h3>Example Handler</h3>
 * <pre>
 * {@code
 * @Component
 * public class MemberCreatedEventHandler {
 *
 *     @ApplicationModuleListener
 *     public void onMemberCreated(MemberCreatedEvent event) {
 *         // Runs asynchronously in new transaction after commit
 *         // Automatic retry on failure
 *         processEvent(event);
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>Configuration</h2>
 * <p>
 * Spring Modulith is configured via {@code application.yml}:
 *
 * <pre>
 * spring.modulith:
 *   events:
 *     enabled: true
 *     completion-mode: UPDATE
 *     republish-incomplete-events-older-than: 5m
 *     delete-completed-events-older-than: 7d
 *   detection-strategy: default
 * </pre>
 *
 * <h3>Configuration Rationale</h3>
 * <ul>
 *   <li><b>5-minute republish threshold:</b> Balances retry frequency with system load.
 *       Most transient failures (network, DB connection) resolve within minutes.</li>
 *   <li><b>7-day retention:</b> Provides sufficient audit trail for debugging while preventing
 *       table bloat. Can be adjusted based on operational requirements.</li>
 *   <li><b>UPDATE completion mode:</b> Updates existing event records rather than deleting
 *       them immediately, preserving history.</li>
 * </ul>
 *
 * <h2>Module Structure</h2>
 * <p>
 * Spring Modulith auto-detects modules from package structure:
 * <ul>
 *   <li>{@code com.klabis.members} - Members bounded context</li>
 *   <li>{@code com.klabis.users} - Users bounded context</li>
 *   <li>{@code com.klabis.events} - Events bounded context</li>
 *   <li>{@code com.klabis.finances} - Finances bounded context</li>
 *   <li>{@code com.klabis.config} - Shared infrastructure (always included in tests)</li>
 * </ul>
 *
 * <h2>References</h2>
 * <ul>
 *   <li><a href="https://docs.spring.io/spring-modulith/reference/">Spring Modulith Documentation</a></li>
 *   <li><a href="https://docs.spring.io/spring-modulith/reference/events.html">Event Publication</a></li>
 *   <li><a href="https://microservices.io/patterns/data/transactional-outbox.html">Transactional Outbox Pattern</a></li>
 * </ul>
 *
 * @see org.springframework.modulith.Modulithic
 * @see org.springframework.modulith.events.ApplicationModuleListener
 */
@Configuration
public class ModulithConfiguration {
    // Configuration is primarily done via application.yml
    // This class serves as documentation and future extension point
}
