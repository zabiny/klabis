## ADDED Requirements

### Requirement: UserId Value Object

The system SHALL implement UserId as a Java record to type-safely represent identifiers for User and Member aggregates.
UserId SHALL wrap a UUID and provide type safety to prevent accidental mixing of IDs from different aggregates. The
record SHALL be immutable and automatically generate equals(), hashCode(), and constructor.

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

## MODIFIED Requirements

### Requirement: User Aggregate

The system SHALL manage user accounts as a separate aggregate from members, linked via registrationNumber. When a Member
is created for a User, the User's UserId SHALL be used as the Member's UserId, establishing a direct identifier
relationship between the aggregates. Users cannot be deleted, only disabled via accountStatus changes.

#### Scenario: User created with credentials and roles

- **WHEN** User.create is called with registrationNumber, password, and roles
- **THEN** User aggregate is created with BCrypt-hashed password
- **AND** User has unique UserId (generated as new UUID)
- **AND** User accountStatus is set to specified value
- **AND** User can exist temporarily without linked Member (until member is created)

#### Scenario: User password changed

- **WHEN** User.changePassword is called with new password
- **THEN** password is BCrypt-hashed and stored
- **AND** User identity (registrationNumber) and UserId are preserved
- **AND** linked Member (if exists) is not affected

#### Scenario: User authorities derived from roles

- **WHEN** User.getAuthorities() is called
- **THEN** system maps roles to authorities (ROLE_ADMIN → MEMBERS:CREATE, MEMBERS:READ, etc.)
- **AND** authorities returned as GrantedAuthority collection

#### Scenario: User account suspended

- **WHEN** User.suspend() is called
- **THEN** accountStatus changed to SUSPENDED
- **AND** subsequent authentication attempts fail
- **AND** existing tokens remain valid until expiration
- **AND** linked Member (if exists) remains in database but user cannot access it

#### Scenario: Member created for user uses user UserId

- **WHEN** a Member is created for an existing User
- **THEN** the Member's UserId is set to the User's UserId
- **AND** both entities share the same UserId instance (same UUID value)
- **AND** the relationship is established through shared identifier
- **AND** registrationNumber remains the linking field for user identity

#### Scenario: User cannot be deleted

- **WHEN** User deletion is attempted
- **THEN** operation is not supported
- **AND** only accountStatus changes (SUSPENDED, DISABLED) are allowed
- **AND** linked Member data is preserved
