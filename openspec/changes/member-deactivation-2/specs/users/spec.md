# Delta Spec: users

## ADDED Requirements

### Requirement: User Account Suspension

The system SHALL support suspending user accounts by setting accountStatus to SUSPENDED and enabled flag to false. Suspended users cannot authenticate via username/password but existing tokens remain valid until natural expiration.

#### Scenario: User account suspended successfully

- **GIVEN** an active User account with accountStatus=ACTIVE
- **WHEN** User.suspend() method is called
- **THEN** a new User instance is returned with accountStatus=SUSPENDED
- **AND** the enabled flag is set to false
- **AND** isAuthenticatable() returns false
- **AND** all other fields (id, username, passwordHash) remain unchanged

#### Scenario: Suspended user cannot authenticate

- **GIVEN** a suspended User account (accountStatus=SUSPENDED, enabled=false)
- **WHEN** authentication is attempted with valid credentials
- **THEN** authentication fails
- **AND** Spring Security rejects the authentication attempt
- **AND** no access token is issued

### Requirement: User Account Reactivation

The system SHALL support reactivating suspended user accounts by setting accountStatus back to ACTIVE and enabled flag to true.

#### Scenario: User account reactivated successfully

- **GIVEN** a suspended User account with accountStatus=SUSPENDED
- **WHEN** User.reactivate() method is called
- **THEN** a new User instance is returned with accountStatus=ACTIVE
- **AND** the enabled flag is set to true
- **AND** isAuthenticatable() returns true
- **AND** authentication succeeds with valid credentials

### Requirement: Cross-Module Event Integration

The system SHALL subscribe to MemberTerminatedEvent and MemberReactivatedEvent from the members bounded context, automatically suspending or reactivating User accounts based on Member lifecycle changes.

#### Scenario: User suspended on member termination

- **GIVEN** an active User account linked to a Member
- **WHEN** MemberTerminatedEvent is received with matching registrationNumber
- **THEN** the User is found by username (registrationNumber)
- **AND** User.suspend() is called
- **AND** the updated User is persisted
- **AND** the operation runs in a separate transaction from Member termination

#### Scenario: User reactivated on member reactivation

- **GIVEN** a suspended User account linked to a terminated Member
- **WHEN** MemberReactivatedEvent is received with matching registrationNumber
- **THEN** the User is found by username (registrationNumber)
- **AND** User.reactivate() is called
- **AND** the updated User is persisted
- **AND** the operation runs in a separate transaction from Member reactivation

#### Scenario: Event handler is idempotent

- **GIVEN** a User account already in target state (SUSPENDED or ACTIVE)
- **WHEN** MemberTerminatedEvent or MemberReactivatedEvent is received again
- **THEN** the event handler checks current User state
- **AND** no changes are made if already in target state
- **AND** no error is thrown
- **AND** operation completes successfully
