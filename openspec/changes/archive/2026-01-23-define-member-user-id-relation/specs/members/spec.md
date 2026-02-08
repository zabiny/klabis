## ADDED Requirements

### Requirement: UserId Value Object

The system SHALL implement UserId as a Java record to type-safely represent identifiers for both User and Member
aggregates. UserId SHALL wrap a UUID and provide type safety to prevent accidental mixing of IDs from different
aggregates. The record SHALL be immutable and automatically generate equals(), hashCode(), and constructor.

#### Scenario: UserId record wraps UUID value

- **WHEN** UserId record is instantiated with a valid UUID
- **THEN** UserId instance is successfully created
- **AND** UserId exposes the UUID value through accessor method (UUID uuid())
- **AND** UserId automatically implements equals() and hashCode() based on the wrapped UUID
- **AND** UserId is immutable (record property)

#### Scenario: UserId prevents null UUID

- **WHEN** UserId record instantiation is attempted with null UUID
- **THEN** IllegalArgumentException is thrown via compact constructor
- **AND** error message indicates UUID cannot be null

#### Scenario: UserId generation from string

- **WHEN** UserId.fromString(String uuidString) static factory method is called with valid UUID string
- **THEN** UserId record instance is created from the parsed UUID
- **AND** if string is invalid UUID format, IllegalArgumentException is thrown

### Requirement: Member-User ID Relationship

Every Member entity SHALL be created with an associated User entity, and the Member SHALL use the User's UserId as its
own UserId. The User is created automatically as part of the member registration flow, and the UserId is shared between
both aggregates.

#### Scenario: User created automatically during member registration

- **WHEN** RegisterMemberCommandHandler processes member registration
- **THEN** a User entity is created first with generated UserId
- **AND** a Member entity is created using the same UserId
- **AND** both entities share the same UserId instance (same UUID value)
- **AND** the relationship is established without requiring additional foreign key fields

#### Scenario: Member uses User's UserId

- **WHEN** a Member is created as part of member registration
- **THEN** the Member's UserId is set to the User's UserId
- **AND** the member and user entities share the same UserId instance
- **AND** no join operation is required for queries by user ID

#### Scenario: Query member by user ID leverages shared identifier

- **WHEN** querying for a member by user's UserId
- **THEN** no join operation is required between User and Member tables
- **AND** query is optimized by using the shared UserId directly
- **AND** performance is improved compared to foreign key lookups

## MODIFIED Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a User entity first, then creating a Member entity that uses
the User's UserId. This flow is internal to the RegisterMemberCommandHandler and does not require API changes.

#### Scenario: Member registration creates User then Member

- **WHEN** authenticated user with MEMBERS:CREATE permission submits member data via POST /api/members
- **THEN** RegisterMemberCommandHandler creates a User entity with generated UserId
- **AND** RegisterMemberCommandHandler creates a Member entity with the same UserId
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
