# members Delta Specification

## MODIFIED Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a User entity first, then creating a Member entity with a unique member identifier that references the associated user.

#### Scenario: Member registration creates User then Member

- **WHEN** authenticated user with MEMBERS:CREATE permission submits member data via POST /api/members
- **THEN** the system creates a User entity with a generated unique identifier
- **AND** the system creates a Member entity with a member identifier that references the same user
- **AND** member is created with generated registration number
- **AND** response includes HAL+FORMS links for viewing, editing, and related actions
- **AND** HTTP 201 Created status is returned with Location header

#### Scenario: Unauthorized user attempts creation

- **WHEN** user without MEMBERS:CREATE permission attempts to create member
- **THEN** HTTP 403 Forbidden is returned
- **AND** response includes error details with problem+json media type

#### Scenario: Invalid data submission

- **WHEN** user submits incomplete or invalid member data
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes validation errors for each invalid field
- **AND** response includes HAL+FORMS template showing required fields

#### Scenario: User creation failure prevents member creation

- **WHEN** User creation fails during member registration
- **THEN** Member creation is not attempted
- **AND** transaction is rolled back
- **AND** error response indicates the failure reason

## ADDED Requirements

### Requirement: Type-Safe Member Identification

The system SHALL enforce type-safe member identification to prevent confusion between member identifiers and other entity identifiers (user, event).

#### Scenario: Member services require member-specific identifier

- **WHEN** calling member update, termination, or query services
- **THEN** services require a member-specific identifier
- **AND** the system prevents accidental use of user or event identifiers where member identifier is required

#### Scenario: Member domain uses member identifier type

- **WHEN** accessing a Member entity's identifier
- **THEN** the identifier is of member-specific type
- **AND** implicit conversion to other identifier types is not allowed

#### Scenario: Member events contain member identifier

- **WHEN** a member-related domain event is published (MemberCreatedEvent, MemberTerminatedEvent)
- **THEN** the event contains member-specific identifier
- **AND** event consumers receive type-safe member reference

### Requirement: Member-User Identifier Relationship

The system SHALL maintain a 1:1 relationship between member and user identifiers while keeping them as distinct types for type safety.

#### Scenario: Member identifier references user identifier

- **WHEN** a Member is created
- **THEN** the member identifier and user identifier reference the same underlying value
- **AND** the system can convert between member and user identifiers when explicitly required
- **AND** no automatic conversion occurs (requires explicit code)

## REMOVED Requirements

None
