## ADDED Requirements

### Requirement: DDD stereotypes are explicitly expressed in code

Every domain class SHALL be annotated with its DDD role: aggregate roots, value objects, domain events, repositories,
and services SHALL carry standardized DDD annotations. Associations between aggregates SHALL be explicitly marked.

#### Scenario: Aggregate root is identifiable

- **WHEN** a developer inspects an aggregate root class
- **THEN** the class carries an `@AggregateRoot` annotation
- **AND** the identity field carries an `@Identity` annotation

#### Scenario: Value object is identifiable

- **WHEN** a developer inspects a value object class
- **THEN** the class carries a `@ValueObject` annotation

#### Scenario: Domain event is identifiable

- **WHEN** a developer inspects a domain event class
- **THEN** the class carries a `@DomainEvent` annotation

#### Scenario: Repository is identifiable

- **WHEN** a developer inspects a repository interface
- **THEN** the interface carries a `@Repository` annotation

#### Scenario: Cross-aggregate reference is identifiable

- **WHEN** an aggregate references another aggregate
- **THEN** the reference field carries an `@Association` annotation and uses an identifier type, not a direct aggregate
  reference

### Requirement: Identifier value objects implement a common type

All identity value objects (e.g., EventId, UserId, MemberId) SHALL implement the `Identifier` marker interface to
provide a unified type for aggregate identifiers.

#### Scenario: Identity value object is typed as Identifier

- **WHEN** a developer inspects an identity value object
- **THEN** the class implements the `Identifier` interface

#### Scenario: Identifier is usable as association target

- **WHEN** an aggregate holds a reference to another aggregate's identity
- **THEN** the identity type is both an `Identifier` and annotated as `@ValueObject`

### Requirement: Hexagonal architecture boundaries are explicitly expressed

Module boundaries SHALL be annotated with hexagonal architecture roles: primary ports, secondary ports, primary
adapters, and secondary adapters.

#### Scenario: Application service is a primary port

- **WHEN** a developer inspects an application service class (e.g., EventManagementService, ManagementService,
  PasswordSetupService)
- **THEN** the class carries a `@PrimaryPort` annotation

#### Scenario: Public query interfaces (Events, Members, Users..) are ports

- **WHEN** a developer inspects an public query interface (e.g., Events, Users, Members, etc..)
- **THEN** the class carries a `@Port` annotation
- **NOTE** these can be used in both `@PrimaryAdapter` while implemented by `@SecondaryAdapter`. That's why we do not
  distinquish primary/secondary there as Primary adapters can't reference secondary ports directly.

#### Scenario: Repository interface is a secondary port

- **WHEN** a developer inspects a repository interface in the persistence package
- **THEN** the interface carries a `@SecondaryPort` annotation

#### Scenario: REST controller is a primary adapter

- **WHEN** a developer inspects a REST controller
- **THEN** the class carries a `@PrimaryAdapter` annotation

#### Scenario: Persistence implementation is a secondary adapter

- **WHEN** a developer inspects a repository adapter or JDBC repository implementation
- **THEN** the class carries a `@SecondaryAdapter` annotation

#### Scenario: Event handler from another module is a primary adapter

- **WHEN** a developer inspects a class handling domain events from another module
- **THEN** the class carries a `@PrimaryAdapter` annotation

### Requirement: Architecture rules are automatically verified

The project SHALL include automated tests that verify architectural rules derived from the annotations. Violations SHALL
cause test failures.

#### Scenario: Aggregate isolation is verified

- **WHEN** the architecture test suite runs
- **THEN** any aggregate that directly references another aggregate (instead of using an identifier) causes a test
  failure

#### Scenario: Dependency direction is verified

- **WHEN** the architecture test suite runs
- **THEN** any domain class that depends on infrastructure classes causes a test failure

#### Scenario: Adapter independence is verified

- **WHEN** the architecture test suite runs
- **THEN** any primary adapter that depends on a secondary adapter (or vice versa) causes a test failure
