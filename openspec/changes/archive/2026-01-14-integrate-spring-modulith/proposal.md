# Change: Integrate Spring Modulith with Outbox Pattern for Async Events

## Why

The current event publishing implementation uses Spring's `ApplicationEventPublisher` synchronously within the same
transaction as aggregate persistence. This creates several reliability and consistency risks:

1. **Dual-write problem**: Events are published before the transaction commits, risking event loss if the commit fails
2. **No guaranteed delivery**: If the application crashes after commit but before event processing, events are lost
3. **No retry mechanism**: Failed event listeners have no automatic retry, leading to data inconsistencies
4. **Scalability limitations**: Event consumers cannot be scaled independently from the main application

Spring Modulith with the transactional outbox pattern solves these problems by persisting events in the same transaction
as the aggregate, then publishing them asynchronously with guaranteed at-least-once delivery.

## What Changes

- **Add Spring Modulith dependencies** to pom.xml for event externalization and JPA-based outbox
- **Create database schema** for `event_publication` outbox table via Flyway migration
- **Configure Spring Modulith** event publication registry with retention and republish policies
- **Refactor event publishing** in repository implementations to leverage outbox pattern
- **Update event listeners** to use `@ApplicationModuleListener` for async processing
- **Add monitoring configuration** for event publication tracking and observability
- **Implement idempotent event handlers** to safely handle duplicate event deliveries
- **Configure module structure** following Spring Modulith's modular monolith patterns

## Impact

**Affected specs:**

- New capability: `event-driven-architecture` (ADDED)

**Affected code:**

- `klabis-backend/pom.xml` - Add Spring Modulith dependencies
- `klabis-backend/src/main/resources/db/migration/V006__create_event_publication_table.sql` - Outbox table schema
- `klabis-backend/src/main/resources/application.yml` - Spring Modulith configuration
- `klabis-backend/src/main/java/com/klabis/members/infrastructure/persistence/MemberRepositoryImpl.java` - Remove manual
  event publishing
- `klabis-backend/src/main/java/com/klabis/members/application/MemberCreatedEventHandler.java` - Update to async +
  idempotent
- `klabis-backend/src/main/java/com/klabis/config/ModulithConfiguration.java` - New configuration class
- Test files for outbox behavior verification

**Benefits:**

- Guaranteed at-least-once event delivery
- Strong consistency between aggregates and events
- Automatic retry mechanism for failed listeners
- Improved observability and monitoring
- Foundation for future event-driven features (notifications, audit logging, integrations)

**Risks:**

- Event handlers must be idempotent (LOW - single handler already implemented correctly)
- Requires database table for outbox (LOW - standard Flyway migration)
- Event delivery is eventually consistent, not immediate (ACCEPTABLE - current async handler already eventual)

**Migration path:**

- Backward compatible - existing event listeners continue working
- No data migration required - new events use outbox going forward
- Can be deployed incrementally - outbox table created first, then handlers updated
