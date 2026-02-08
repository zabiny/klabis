# Transactional Outbox Pattern Implementation

> **⚠️ Historical Document**
>
> This document describes the **migration proposal** for implementing Spring Modulith with the transactional outbox
> pattern.
>
> **Status:** ✅ Migration completed on January 13, 2026
>
> **For current implementation documentation, see:**
> - [EVENT-DRIVEN-ARCHITECTURE.md](./EVENT-DRIVEN-ARCHITECTURE.md) - Current implementation details
> - [OPERATIONS_RUNBOOK.md](./OPERATIONS_RUNBOOK.md) - Event monitoring and troubleshooting
>
> **Purpose:** This document is kept for historical reference, showing the migration journey from the old event handling
> approach to Spring Modulith.

---

## Original Proposal Status

**Status:** ✅ **IMPLEMENTED** (Spring Modulith Integration)

**Implementation Date:** January 13, 2026

**Related Changes:** [openspec/changes/integrate-spring-modulith](../openspec/changes/integrate-spring-modulith/)

## Overview

The transactional outbox pattern is implemented using **Spring Modulith 1.4.6**, which provides:

- Automatic event persistence to `EVENT_PUBLICATION` table
- Asynchronous event processing after transaction commit
- Automatic retry on failures
- Event publication monitoring via Spring Boot Actuator

## Current Implementation

**Spring Modulith provides:**

- Events are persisted atomically with aggregate data in the same transaction
- Events are published to listeners AFTER transaction commits
- Background thread processes events asynchronously
- Failed events are automatically retried
- Event completion is tracked in `event_publication` table

**Current Behavior:**
✅ **Pros:**

- Simple implementation
- Defensive: If event listener fails, transaction rolls back
- No additional dependencies

❌ **Cons:**

- Events published before commit (consistency risk if commit fails)
- No guaranteed delivery if application crashes after commit
- Dual-write problem if listeners write to external systems
- No retry mechanism for failed event processing
- Cannot scale event consumers independently

## Why Outbox Pattern?

The transactional outbox pattern solves the **dual-write problem**:

```
Without Outbox:
1. Write to database ✅
2. Publish event to message broker ❌ (fails)
   → Data saved but event lost!

Or:
1. Publish event to message broker ✅
2. Write to database ❌ (fails, transaction rolls back)
   → Event published but no data!
```

With outbox pattern:

```
1. Write aggregate to database ✅
2. Write event to outbox table in SAME transaction ✅
3. Transaction commits (atomic)
4. Background process reads outbox and publishes events
5. Mark events as published
   → Guaranteed at-least-once delivery!
```

## Proposed Implementation with Spring Modulith

### Phase 1: Add Spring Modulith Dependencies

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-core</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-starter-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.modulith</groupId>
    <artifactId>spring-modulith-events-api</artifactId>
</dependency>
```

### Phase 2: Database Schema for Event Publication

Spring Modulith will auto-create the outbox table:

```sql
CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    event_type VARCHAR(512) NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    serialized_event TEXT NOT NULL,
    completion_date TIMESTAMP
);

CREATE INDEX idx_event_publication_completion
ON event_publication(completion_date);
```

### Phase 3: Enable Event Externalization

```java
@Configuration
@EnableAsync
public class ModulithConfiguration {

    @Bean
    public ApplicationModuleInitializer applicationModuleInitializer(
            ApplicationContext context) {
        return ApplicationModuleInitializer.from(context);
    }
}
```

### Phase 4: Update Event Listeners

**Current approach** (synchronous, no outbox):

```java
@Component
public class WelcomeEmailSender {

    @EventListener  // Executes within transaction
    public void onMemberCreated(MemberCreatedEvent event) {
        // Send email
    }
}
```

**New approach** (asynchronous with outbox):

```java
@Component
public class WelcomeEmailSender {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberCreated(MemberCreatedEvent event) {
        // Send email - executes AFTER commit
        // Spring Modulith ensures delivery even if app crashes
    }
}
```

Or with Spring Modulith's annotation:

```java
@Component
public class WelcomeEmailSender {

    @ApplicationModuleListener  // Spring Modulith annotation
    public void onMemberCreated(MemberCreatedEvent event) {
        // Automatically uses outbox pattern
        // Guaranteed delivery with retries
    }
}
```

### Phase 5: Event Publication Configuration

```yaml
# application.yml
spring:
  modulith:
    events:
      # Enable event publication registry (outbox)
      enabled: true
      # Republish events that haven't completed after 5 minutes
      republish-incomplete-events-older-than: 5m
      # Delete completed events after 7 days
      delete-completed-events-older-than: 7d
```

### Phase 6: Remove Manual Event Publishing

**Remove from RegisterMemberCommandHandler:**

```java
// OLD CODE (to be removed):
savedMember.getDomainEvents().forEach(eventPublisher::publishEvent);
savedMember.clearDomainEvents();

// NEW CODE (Spring Modulith auto-publishes from outbox):
// Events are automatically published from outbox table
// No manual publishing needed
```

**Keep event registration in Member aggregate:**

```java
// Member.create() still registers events
member.registerEvent(MemberCreatedEvent.fromMember(member));
```

**Update MemberEntity to publish events via JPA lifecycle:**

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class MemberEntity {

    @PostPersist
    @PostUpdate
    private void publishEvents() {
        // Spring Modulith will intercept and store in outbox
        getDomainEvents().forEach(eventPublisher::publishEvent);
        clearDomainEvents();
    }
}
```

## Benefits of Spring Modulith Approach

✅ **Reliability:**

- Guaranteed at-least-once delivery
- Events survive application crashes
- Automatic retry mechanism

✅ **Consistency:**

- Events stored in same transaction as aggregate
- No dual-write problem
- Strong consistency guarantees

✅ **Scalability:**

- Event consumers can run asynchronously
- Can scale processing independently
- Background job processes outbox

✅ **Observability:**

- Built-in event publication tracking
- Can query which events are pending
- Monitoring and alerting support

✅ **Testing:**

- Can verify events were published
- Integration tests with outbox assertions
- Clear separation of concerns

## Migration Path

### Step 1: Add Dependencies

- Add Spring Modulith dependencies to pom.xml
- Add flyway migration for event_publication table

### Step 2: Enable Outbox

- Enable Spring Modulith event publication registry
- Configure retention and republish settings

### Step 3: Update Event Listeners

- Annotate with `@ApplicationModuleListener` or `@TransactionalEventListener(AFTER_COMMIT)`
- Make listeners idempotent (they may receive duplicate events)

### Step 4: Remove Manual Publishing

- Remove `eventPublisher::publishEvent` from command handlers
- Let Spring Modulith handle publication from outbox

### Step 5: Testing

- Add integration tests verifying outbox behavior
- Test event delivery after application restart
- Test retry mechanism for failed listeners

## Related Tasks

This should be implemented together with:

- **Task 3.3**: Email service implementation (JavaMailSender)
- **Section 6**: Event handlers (MemberCreatedEventHandler, welcome email trigger)
- **Section 10** (future): GDPR audit logging via events

## References

- [Spring Modulith Documentation](https://spring.io/projects/spring-modulith)
- [Spring Modulith Event Publication](https://docs.spring.io/spring-modulith/reference/events.html)
- [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Domain Events by Martin Fowler](https://martinfowler.com/eaaDev/DomainEvent.html)

## Decision

**Status:** APPROVED - Deferred to future implementation
**Responsible:** Backend Team
**Timeline:** To be implemented with email service feature (task 3.3)
**Risk:** LOW - Current implementation is acceptable for MVP
