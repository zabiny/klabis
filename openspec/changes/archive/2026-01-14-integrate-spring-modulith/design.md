# Design: Spring Modulith Integration with Outbox Pattern

## Context

The Klabis backend follows Domain-Driven Design principles with bounded contexts (Members, Users, Events, Finances).
Currently, domain events like `MemberCreatedEvent` are published synchronously using Spring's
`ApplicationEventPublisher` within the same transaction.

**Current architecture:**

```
Repository.save() → Publish events → Commit transaction
```

**Problem:** This creates the dual-write problem where events are published before commit. If the commit fails, events
are already published. If the application crashes after commit, events may not be processed.

**Stakeholders:**

- Development team (implementation and maintenance)
- Operations team (monitoring event delivery)
- Business stakeholders (reliable notifications and integrations)

**Constraints:**

- Must maintain ACID guarantees for financial and member data
- Must support 100+ concurrent users during peak loads
- Database is PostgreSQL (production) and H2 (development/test)
- Existing event handlers must continue working without breaking changes

## Goals / Non-Goals

**Goals:**

1. Implement transactional outbox pattern for reliable event delivery
2. Enable guaranteed at-least-once event processing
3. Provide automatic retry mechanism for failed event handlers
4. Support eventual consistency between bounded contexts
5. Add monitoring and observability for event publication
6. Lay foundation for future async integrations (notifications, webhooks, audit logs)

**Non-Goals:**

1. Migrate to microservices or distributed messaging (RabbitMQ, Kafka) - keeping modular monolith
2. Implement event sourcing or CQRS - only using events for cross-context communication
3. Change domain model or aggregate design - purely infrastructure concern
4. Replace Spring's event publishing mechanism entirely - augment it with persistence
5. Support distributed transactions or saga patterns - single database only

## Decisions

### Decision 1: Use Spring Modulith over custom outbox implementation

**Rationale:**

- Spring Modulith provides production-ready outbox implementation
- Integrates seamlessly with Spring Boot and JPA
- Includes monitoring, retry logic, and event cleanup out of the box
- Supports modular monolith architecture we're already following
- Active Spring project with community support

**Alternatives considered:**

1. **Custom outbox implementation** - More control but significant development and maintenance overhead
2. **Debezium CDC** - Requires Kafka infrastructure, too heavy for current needs
3. **Spring Cloud Stream** - Designed for microservices, not modular monoliths
4. **Axon Framework** - Full event sourcing framework, excessive for our use case

**Trade-offs:**

- ✅ Simplicity: Minimal code changes required
- ✅ Reliability: Battle-tested implementation
- ✅ Maintainability: Managed by Spring team
- ❌ Dependency: Adds external framework dependency (acceptable - it's official Spring project)
- ❌ Learning curve: Team needs to understand Spring Modulith concepts (low - similar to Spring)

### Decision 2: JPA-based event publication registry

**Rationale:**

- Leverages existing PostgreSQL database and JPA stack
- Events stored in same database as aggregates (single transaction)
- No additional infrastructure required (no message broker)
- Simple to monitor and debug (SQL queries)

**Alternatives considered:**

1. **MongoDB event store** - Requires separate database, adds complexity
2. **Redis-based outbox** - Not durable enough for financial data
3. **Kafka or RabbitMQ** - Over-engineering for modular monolith, infrastructure overhead

**Implementation details:**

```sql
CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    event_type VARCHAR(512) NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    serialized_event TEXT NOT NULL,
    completion_date TIMESTAMP
);
```

### Decision 3: Event publication lifecycle configuration

**Configuration:**

```yaml
spring:
  modulith:
    events:
      enabled: true
      completion-mode: UPDATE  
      republish-incomplete-events-older-than: 5m
      delete-completed-events-older-than: 7d
```

**Rationale:**

- **5-minute republish window**: Balances retry frequency with system load
    - Most transient failures (network, DB connection) resolve within minutes
    - Not too aggressive to overwhelm failing services
- **7-day retention**: Provides audit trail while preventing table bloat
    - Sufficient for debugging and monitoring
    - Can reconstruct event timeline for recent incidents
    - Old events can be archived separately if needed

**Alternatives considered:**

1. **Immediate retry (< 1 minute)**: Too aggressive, may amplify failures
2. **Long retention (30+ days)**: Unnecessary table bloat, slower queries
3. **No retention**: Lost audit trail, harder debugging

### Decision 4: Idempotent event handler pattern

**Pattern:**

```java
@Component
public class MemberCreatedEventHandler {

    @ApplicationModuleListener
    public void onMemberCreated(MemberCreatedEvent event) {
        // Check if already processed (idempotency)
        if (alreadyProcessed(event.getEventId())) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }

        // Process event
        processEvent(event);

        // Mark as processed
        markProcessed(event.getEventId());
    }
}
```

**Rationale:**

- At-least-once delivery guarantees mean handlers may receive duplicates
- Idempotency ensures duplicate processing is safe
- `eventId` in `MemberCreatedEvent` provides natural deduplication key

**Note for current implementation:**

- `MemberCreatedEventHandler` currently sends password setup email
- Email sending is naturally idempotent (user receives same link multiple times)
- For MVP, no additional idempotency checks needed
- Future handlers (payment processing, audit logs) MUST implement explicit checks

**Idempotency implementation patterns for future handlers:**

**Option 1: Database table (recommended for financial operations)**

```java
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final ProcessedEventRepository processedEventRepository;

    @ApplicationModuleListener
    public void onPaymentRequested(PaymentRequestedEvent event) {
        // Check if already processed
        if (processedEventRepository.existsById(event.getEventId())) {
            log.info("Event {} already processed, skipping", event.getEventId());
            return;
        }

        // Process payment
        paymentService.processPayment(event);

        // Mark as processed
        processedEventRepository.save(new ProcessedEvent(event.getEventId()));
    }
}
```

**Option 2: Redis cache (for high-throughput scenarios)**

```java
@Component
@RequiredArgsConstructor
public class HighThroughputEventHandler {

    private final RedisTemplate<String, String> redisTemplate;

    @ApplicationModuleListener
    void on(SomeEvent event) {
        // Try to acquire idempotency lock
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent("event:" + event.id(), "processed", Duration.ofDays(7));

        if (Boolean.FALSE.equals(acquired)) {
            log.debug("Event {} already processed", event.id());
            return;
        }

        // Process event
        handleEvent(event);
    }
}
```

**Option 3: Natural idempotency (for email and notifications)**

```java
@Component
public class EmailEventHandler {

    @ApplicationModuleListener
    public void onMemberCreated(MemberCreatedEvent event) {
        // Email links are naturally idempotent - sending same link multiple times is safe
        // Recipient email service handles duplicate sends (idempotent send API)
        emailService.sendPasswordSetupEmail(event.getEmail(), event.getToken());
    }
}
```

### Decision 5: Module structure following Spring Modulith conventions

**Package structure:**

```
com.klabis/
├── members/          # Members module (bounded context)
│   ├── domain/
│   ├── application/
│   ├── infrastructure/
│   └── presentation/
├── users/           # Users module (bounded context)
│   ├── domain/
│   ├── application/
│   └── infrastructure/
└── config/          # Shared configuration
```

**Rationale:**

- Matches existing package structure (no refactoring needed)
- Spring Modulith auto-detects modules from package structure
- Clear module boundaries for dependency verification
- Supports future module-level testing and documentation

**Spring Modulith module API:**

- Events in `domain/` package are public API
- Application services and repositories are module-private
- Cross-module communication ONLY via domain events

**@Modulithic annotation on main application class:**

```java
@Modulithic(
    systemName = "Klabis Membership Management",
    sharedModules = "config"  // Always included in module tests
)
@SpringBootApplication
public class KlabisApplication {
    public static void main(String[] args) {
        SpringApplication.run(KlabisApplication.class, args);
    }
}
```

**Configuration:**

```yaml
spring:
  modulith:
    detection-strategy: default  # Auto-detect direct sub-packages as modules
```

### Decision 5.1: Event listener annotation choice

**Pattern:**

```java
@Component
public class MemberCreatedEventHandler {

    @ApplicationModuleListener
    public void onMemberCreated(MemberCreatedEvent event) {
        // Process event
    }
}
```

**Rationale for `@ApplicationModuleListener` over `@TransactionalEventListener`:**

- Standard for Spring Modulith
- contains best practises for events communication between modules
- easier to grasp for developers without Spring knowledge

**Note:** `@ApplicationModuleListener` (Spring Modulith v1.2+) is equivalent to combining:

- `@Async` - asynchronous execution
- `@Transactional(propagation = REQUIRES_NEW)` - new transaction
- `@TransactionalEventListener` - listens after commit

## Risks / Trade-offs

### Risk 1: Event handler failures causing event buildup

**Risk:** If event handlers consistently fail, outbox table grows indefinitely

**Likelihood:** LOW - current handlers are simple and stable

**Mitigation:**

1. Configure maximum retry attempts (via Spring Retry)
2. Implement dead letter queue for persistently failing events
3. Monitor outbox table size with alerts
4. Add circuit breakers for external service calls in handlers

**Monitoring:**

```sql
-- Alert if > 1000 incomplete events older than 1 hour
SELECT COUNT(*) FROM event_publication
WHERE completion_date IS NULL
AND publication_date < NOW() - INTERVAL '1 hour';
```

### Risk 2: Database performance impact from outbox table

**Risk:** Large outbox table may slow down event queries and cleanup

**Likelihood:** LOW - with 7-day retention, table stays manageable

**Mitigation:**

1. Index on `completion_date` for efficient cleanup queries
2. Partition table by publication_date if volume grows
3. Configure aggressive cleanup policy (7 days is conservative)
4. Monitor query performance in production

**Capacity planning:**

- Assume 1000 members registered per year
- Each registration = 1 event
- With 7-day retention: ~20 events max in outbox
- Negligible performance impact

### Risk 3: Breaking change to event listener signature

**Risk:** Updating listener annotations from `@TransactionalEventListener` to `@ApplicationModuleListener` may break
existing code

**Likelihood:** LOW - annotation change is straightforward and well-supported

**Mitigation:**

- `@ApplicationModuleListener` is functionally equivalent to `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` +
  `@Transactional(propagation = REQUIRES_NEW)`
- Only annotation change required, no method signature changes
- Spring Modulith provides backward compatibility
- Existing handler logic remains unchanged
- Fully backward compatible

### Risk 4: Event ordering guarantees

**Risk:** Async processing may deliver events out of order

**Likelihood:** LOW - single event type currently, natural ordering preserved

**Mitigation:**

1. Spring Modulith preserves publication order by default
2. For critical ordering (future), implement sequence numbers in events
3. Design aggregates to be order-independent where possible
4. Use saga pattern for complex workflows (future consideration)

**Current status:** Single event type (`MemberCreatedEvent`) has no ordering dependencies

## Migration Plan

### Phase 1: Add Infrastructure (Non-breaking)

**Steps:**

1. Add Spring Modulith dependencies to `pom.xml`
2. Create Flyway migration for `event_publication` table
3. Add Spring Modulith configuration to `application.yml`
4. Deploy and verify outbox table created

**Validation:**

- Database migration runs successfully
- Application starts without errors
- Outbox table exists with correct schema

**Rollback:** Remove dependencies, drop table via migration

### Phase 2: Enable Event Persistence (Non-breaking)

**Steps:**

1. Keep existing `ApplicationEventPublisher` calls in repositories
2. Spring Modulith automatically intercepts and persists events
3. Events published both via outbox AND synchronously (temporary redundancy)
4. Monitor both paths in production

**Validation:**

- Events appear in `event_publication` table
- Event handlers still execute
- No duplicate processing

**Rollback:** Disable Spring Modulith config, continue with sync publishing

### Phase 3: Remove Manual Publishing (Breaking for infrastructure only)

**Steps:**

1. Remove `eventPublisher.publishEvent()` calls from repositories
2. Remove `member.clearDomainEvents()` from repositories
3. Let Spring Modulith handle all event publishing via outbox
4. Monitor outbox processing

**Validation:**

- Events still processed correctly
- Outbox cleanup runs on schedule
- No events lost during transition

**Rollback:** Re-add manual publishing code (git revert)

### Phase 4: Add Monitoring and Observability

**Steps:**

1. Add metrics for event publication counts
2. Configure alerts for incomplete events
3. Add logging for event processing lifecycle
4. Create dashboard for event delivery monitoring

**Validation:**

- Metrics appear in monitoring system
- Alerts fire correctly for test failures
- Dashboards show real-time event status

## Open Questions

1. **Q: Should we implement a dead letter queue for permanently failed events?**
    - A: Not for MVP. Add when we have more complex event handlers or external integrations

2. **Q: Do we need event versioning for backward compatibility?**
    - A: Not yet. Current events are simple and only used internally. Consider when publishing to external systems

3. **Q: Should we expose event publication metrics via Spring Boot Actuator?**
    - A: YES. Add `/actuator/modulith` endpoint for operations team

4. **Q: What happens to in-flight events during deployment?**
    - A: Spring Modulith handles gracefully - incomplete events republished after restart

5. ~~**Q: Should we use Spring Modulith's `@ApplicationModuleListener` instead of `@TransactionalEventListener`?**~~
    - **DECIDED**: Yes, use `@ApplicationModuleListener`. See Decision 5.1 for detailed rationale.

6. **Q: Should we externalize events to Kafka/RabbitMQ for future integrations?**
    - A: Not for MVP. Spring Modulith supports event externalization via `@Externalized` annotation when we need to
      publish to external systems (Kafka, AMQP). Current events are internal-only between modules within the same
      application. Can add externalization later without changing event handlers.

## Implementation Notes

### Testing Strategy

**Unit tests:**

- Test domain events are created correctly
- Test event handler logic in isolation (mock external services)

**Integration tests:**

- Verify events persisted to outbox table
- Test event delivery after transaction commit
- Test retry mechanism for failed handlers
- Test idempotency for duplicate events

**E2E tests:**

- Register member and verify password setup email sent
- Test event delivery survives application restart
- Test cleanup of completed events

### Dependencies

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
    <version>1.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jpa</artifactId>
    <version>1.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-events-api</artifactId>
    <version>1.1.0</version>
</dependency>
```

**Test dependencies:**

```xml
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-test</artifactId>
    <version>1.1.0</version>
    <scope>test</scope>
</dependency>
```

## References

- [Spring Modulith Documentation](https://docs.spring.io/spring-modulith/reference/)
- [Spring Modulith Event Publication](https://docs.spring.io/spring-modulith/reference/events.html)
- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Domain Events by Martin Fowler](https://martinfowler.com/eaaDev/DomainEvent.html)
- [Spring Modulith GitHub](https://github.com/spring-projects/spring-modulith)
