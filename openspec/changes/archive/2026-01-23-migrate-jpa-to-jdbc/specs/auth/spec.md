# Authentication Specification Delta

## MODIFIED Requirements

### Requirement: User Aggregate Persistence

The system SHALL persist User aggregates using Spring Data JDBC with direct domain object mapping, eliminating the need
for separate entity classes and manual mapping between domain and persistence layers.

#### Scenario: User aggregate persisted to database

- **WHEN** User aggregate is saved via UserRepository
- **THEN** Spring Data JDBC persists User directly without entity mapping
- **AND** User roles collection stored in user_roles table via @MappedCollection
- **AND** User authorities serialized as JSON string column
- **AND** Optimistic locking version field incremented automatically
- **AND** Audit fields (createdAt, lastModifiedAt) populated via @CreatedDate and @LastModifiedDate

#### Scenario: User aggregate loaded from database

- **WHEN** User is retrieved by ID or registrationNumber
- **THEN** Spring Data JDBC reconstructs User domain object directly
- **AND** User roles collection loaded from user_roles table
- **AND** User authorities deserialized from JSON string
- **AND** Version field loaded for optimistic locking
- **AND** User implements Persistable<UUID> for new entity detection

#### Scenario: User roles updated and persisted

- **WHEN** User.assignRole() is called and User is saved
- **THEN** Spring Data JDBC cascades changes to user_roles table
- **AND** Removed roles deleted from user_roles table
- **AND** Added roles inserted into user_roles table
- **AND** All changes occur in same transaction

#### Scenario: Optimistic locking prevents concurrent modifications

- **WHEN** two transactions load same User and both attempt to save
- **THEN** first save succeeds and increments version
- **AND** second save fails with OptimisticLockingFailureException
- **AND** application can retry or notify user of conflict

#### Scenario: Domain events published on User save

- **WHEN** User aggregate publishes domain event (e.g., UserActivatedEvent)
- **THEN** Spring Data JDBC detects events via AbstractAggregateRoot
- **AND** Events stored in event_publication table (Spring Modulith)
- **AND** Events and User changes committed in same transaction
- **AND** Event listeners notified after transaction commit

---

## REMOVED Requirements

### Requirement: JPA Entity Mapping for User

**Reason**: Spring Data JDBC eliminates the need for separate entity classes (UserEntity) and manual mapper classes (
UserMapper). Domain objects are persisted directly.

**Migration**:

- Remove UserEntity.java
- Remove UserMapper.java
- Remove UserJpaRepository.java
- User domain class now implements Persistable<UUID> and includes Spring Data JDBC annotations
- UserRepository implementation delegates directly to UserJdbcRepository
