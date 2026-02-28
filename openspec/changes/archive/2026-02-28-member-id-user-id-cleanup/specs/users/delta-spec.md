# users Delta Specification

## MODIFIED Requirements

### Requirement: UserCreatedEvent Contains Type-Safe User Identifier

The system SHALL publish `UserCreatedEvent` with a type-safe user identifier field.

#### Scenario: UserCreatedEvent published with type-safe identifier

- **WHEN** a new User is created
- **THEN** a `UserCreatedEvent` is published with a user-specific identifier type
- **AND** event consumers receive type-safe user identifier that can be used directly without type conversion

#### Scenario: UserCreatedEvent identifier access

- **WHEN** accessing the user identifier from `UserCreatedEvent`
- **THEN** the identifier is of user-specific type
- **AND** no manual conversion from generic types is required by event consumers

## ADDED Requirements

### Requirement: Type-Safe User Identification

The system SHALL enforce type-safe user identification to prevent confusion between user identifiers and other entity identifiers.

#### Scenario: User services require user-specific identifier

- **WHEN** calling user permission, authentication, or query services
- **THEN** services require a user-specific identifier
- **AND** the system prevents accidental use of member or event identifiers where user identifier is required

#### Scenario: User domain uses user identifier type

- **WHEN** accessing a User entity's identifier
- **THEN** the identifier is of user-specific type
- **AND** implicit conversion to other identifier types is not allowed

## REMOVED Requirements

None
