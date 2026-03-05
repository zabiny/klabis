# events Delta Specification

## MODIFIED Requirements

### Requirement: Event Registration with Type-Safe Identifiers

The system SHALL use type-specific identifiers in event registration operations to prevent confusion between different entity types.

#### Scenario: Register for event uses type-specific identifiers

- **WHEN** authenticated member registers for an event via POST /api/events/{eventId}/registrations
- **THEN** the registration service uses event-specific and member-specific identifiers
- **AND** the system prevents accidental use of incorrect identifier types

#### Scenario: Unregister from event uses type-specific identifiers

- **WHEN** authenticated member cancels event registration
- **THEN** the service uses event-specific and member-specific identifiers

#### Scenario: Registration exceptions provide type-safe context

- **WHEN** throwing exceptions for registration issues (duplicate, not found)
- **THEN** exceptions contain type-specific identifiers for both member and event
- **AND** error context clearly identifies the entities involved

## ADDED Requirements

### Requirement: Type-Safe Event Identification

The system SHALL enforce type-safe event identification to prevent confusion between event identifiers and other entity identifiers.

#### Scenario: Event services require event-specific identifier

- **WHEN** calling event management services (update, publish, cancel, finish, query)
- **THEN** services require an event-specific identifier
- **AND** the system prevents accidental use of member or user identifiers where event identifier is required

#### Scenario: Event domain uses event identifier type

- **WHEN** accessing an Event entity's identifier
- **THEN** the identifier is of event-specific type
- **AND** implicit conversion to other identifier types is not allowed

### Requirement: Event Identifier Consistency

The system SHALL maintain consistent type-safe identifier usage across all event-related operations.

#### Scenario: Event operations use consistent identifier type

- **WHEN** performing any operation on an event (registration, management, querying)
- **THEN** all services and methods use the same event-specific identifier type
- **AND** no generic identifier types are used in the domain or service layer

## REMOVED Requirements

None
