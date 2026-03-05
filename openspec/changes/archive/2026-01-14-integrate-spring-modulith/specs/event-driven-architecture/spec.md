# Event-Driven Architecture Specification

## ADDED Requirements

### Requirement: Transactional Outbox for Event Publication

The system SHALL implement the transactional outbox pattern to ensure reliable, at-least-once delivery of domain events
across bounded contexts.

#### Scenario: Event persisted atomically with aggregate

- **WHEN** a domain aggregate is saved to the database (e.g., Member, User)
- **AND** the aggregate has registered domain events
- **THEN** the events SHALL be persisted to the outbox table in the same database transaction as the aggregate
- **AND** the transaction SHALL commit atomically (both aggregate and events saved together or neither)

#### Scenario: Event published after transaction commit

- **WHEN** a transaction containing domain events commits successfully
- **THEN** Spring Modulith SHALL publish the events to registered listeners
- **AND** event publication SHALL occur asynchronously after the commit
- **AND** failed event publication SHALL NOT cause transaction rollback

#### Scenario: Event delivery survives application restart

- **WHEN** an event is persisted to the outbox table
- **AND** the application crashes or restarts before the event is published
- **THEN** the event SHALL be automatically republished when the application restarts
- **AND** no events SHALL be lost due to application failures

### Requirement: Event Publication Registry

The system SHALL maintain an event publication registry in the database to track event publication status and enable
monitoring.

#### Scenario: Event publication record created

- **WHEN** a domain event is persisted via the outbox pattern
- **THEN** a record SHALL be created in the `event_publication` table
- **AND** the record SHALL contain the event type, listener ID, publication date, and serialized event data
- **AND** the completion_date column SHALL be NULL until the event is successfully processed

#### Scenario: Event publication record marked complete

- **WHEN** an event listener successfully processes an event
- **THEN** the corresponding `event_publication` record SHALL be updated
- **AND** the completion_date column SHALL be set to the current timestamp
- **AND** the record SHALL be eligible for cleanup after the retention period

#### Scenario: Incomplete events republished

- **WHEN** an event has been in the outbox for longer than the configured republish threshold (5 minutes)
- **AND** the completion_date is still NULL
- **THEN** Spring Modulith SHALL automatically republish the event to the listener
- **AND** the event SHALL continue to be retried until successfully processed or manually removed

### Requirement: Event Publication Cleanup

The system SHALL automatically clean up completed events from the outbox table to prevent unbounded table growth.

#### Scenario: Completed events deleted after retention period

- **WHEN** the event publication cleanup job runs
- **AND** an event has a non-NULL completion_date
- **AND** the completion_date is older than the configured retention period (7 days)
- **THEN** the event publication record SHALL be deleted from the outbox table
- **AND** the event data SHALL no longer be available for replay

#### Scenario: Incomplete events retained indefinitely

- **WHEN** an event publication has a NULL completion_date
- **THEN** the event SHALL NOT be deleted by the cleanup job
- **AND** the event SHALL remain in the outbox for retry attempts
- **AND** the event SHOULD be investigated if incomplete for more than 24 hours

### Requirement: Idempotent Event Handlers

Event listeners SHALL be idempotent to safely handle duplicate event deliveries due to the at-least-once delivery
guarantee.

#### Scenario: Duplicate event ignored

- **WHEN** an event listener receives an event
- **AND** the event has already been processed (detected via event ID)
- **THEN** the event listener SHALL skip processing
- **AND** no duplicate side effects SHALL occur (e.g., no duplicate emails sent)
- **AND** the event SHALL be marked as successfully processed

#### Scenario: Event processed exactly once logically

- **WHEN** an event is delivered multiple times due to retries
- **THEN** the business logic SHALL execute exactly once
- **AND** subsequent deliveries SHALL be no-ops
- **AND** the system SHALL remain in a consistent state

### Requirement: Event Publication Monitoring

The system SHALL provide observability into event publication status for operations and debugging.

#### Scenario: Event publication metrics exposed

- **WHEN** the application is running
- **THEN** metrics SHALL be exposed for total events published
- **AND** metrics SHALL include count of incomplete events
- **AND** metrics SHALL include count of events pending retry
- **AND** metrics SHALL be accessible via Spring Boot Actuator

#### Scenario: Event publication history queryable

- **WHEN** an operator needs to debug event delivery issues
- **THEN** the operator SHALL be able to query the `event_publication` table
- **AND** the query SHALL return all events within the retention period
- **AND** the query results SHALL include event type, publication date, completion status, and serialized event data

### Requirement: Async Event Processing

Event listeners SHALL execute asynchronously after the aggregate transaction commits to avoid blocking the command
handler.

#### Scenario: Event listener executes after commit

- **WHEN** a transaction commits with domain events
- **THEN** event listeners SHALL execute in a separate thread
- **AND** event listener execution SHALL NOT block the command handler response
- **AND** event listener failures SHALL NOT affect the committed transaction

#### Scenario: Command handler responds before event processing completes

- **WHEN** a command handler saves an aggregate with domain events
- **THEN** the command handler SHALL return a response to the client
- **AND** the response SHALL indicate success based on aggregate persistence
- **AND** event processing MAY still be in progress when the response is returned

### Requirement: Spring Modulith Configuration

The system SHALL be configured to use Spring Modulith's event externalization features with appropriate settings for the
application's needs.

#### Scenario: Event publication enabled

- **WHEN** the application starts
- **THEN** Spring Modulith event publication SHALL be enabled
- **AND** the event publication registry SHALL be initialized
- **AND** the outbox table SHALL exist in the database

#### Scenario: Republish threshold configured

- **WHEN** Spring Modulith is configured
- **THEN** the republish threshold SHALL be set to 5 minutes
- **AND** events incomplete for longer than 5 minutes SHALL be automatically retried
- **AND** the retry interval SHALL balance responsiveness with system load

#### Scenario: Retention period configured

- **WHEN** Spring Modulith is configured
- **THEN** the retention period for completed events SHALL be set to 7 days
- **AND** completed events older than 7 days SHALL be automatically deleted
- **AND** the retention period SHALL provide sufficient audit trail while preventing table bloat

### Requirement: Database Schema for Outbox

The system SHALL include a database table to persist event publication records as part of the transactional outbox
pattern.

#### Scenario: Event publication table exists

- **WHEN** the database schema is initialized or migrated
- **THEN** an `event_publication` table SHALL exist
- **AND** the table SHALL have columns for id, event_type, listener_id, publication_date, serialized_event, and
  completion_date
- **AND** the table SHALL have an index on completion_date for efficient cleanup queries

#### Scenario: Event publication table supports required operations

- **WHEN** events are stored in the outbox table
- **THEN** the table schema SHALL support atomic inserts within aggregate transactions
- **AND** the table SHALL support efficient queries for incomplete events
- **AND** the table SHALL support batch deletion of completed events

### Requirement: Module Boundaries and Dependencies

The system SHALL enforce module boundaries using Spring Modulith to prevent inappropriate cross-module dependencies.

#### Scenario: Cross-module communication via events only

- **WHEN** one bounded context (module) needs to react to changes in another
- **THEN** communication SHALL occur exclusively via domain events
- **AND** direct method calls between modules SHALL NOT be allowed
- **AND** Spring Modulith SHALL verify module dependency rules

#### Scenario: Internal module implementation hidden

- **WHEN** a module defines internal application services or repositories
- **THEN** those components SHALL NOT be accessible from other modules
- **AND** only domain events in the domain package SHALL be public module API
- **AND** Spring Modulith SHALL enforce encapsulation at compile time and runtime
