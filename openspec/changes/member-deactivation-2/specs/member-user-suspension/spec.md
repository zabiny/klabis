# Member User Suspension Specification

## Purpose

This specification defines requirements for automatic User account suspension when Member membership is terminated. It ensures that terminated members cannot authenticate or access protected API endpoints, maintaining security and access control consistency across the system.

## Requirements

### Requirement: User Suspension on Member Termination

The system SHALL automatically suspend the corresponding User account when a Member's membership is terminated. The suspension shall be triggered by domain event and executed in a separate transaction.

#### Scenario: User account suspended when member terminated

- **GIVEN** an active Member with corresponding active User account
- **WHEN** the Member's membership is terminated via POST /api/members/{id}/terminate
- **THEN** the MemberTerminatedEvent is published with registrationNumber
- **AND** the User account accountStatus is set to SUSPENDED
- **AND** the User account enabled flag is set to false
- **AND** the User account isAuthenticatable() returns false
- **AND** subsequent authentication attempts fail

#### Scenario: User suspension is idempotent

- **GIVEN** a terminated Member with already suspended User account
- **WHEN** the MemberTerminatedEvent is processed again (e.g., retry)
- **THEN** the User account remains SUSPENDED
- **AND** no error is thrown
- **AND** the operation completes successfully

#### Scenario: Missing user account handled gracefully

- **GIVEN** a Member without a corresponding User account
- **WHEN** the Member's membership is terminated
- **THEN** the MemberTerminatedEvent is published
- **AND** the event handler logs a warning
- **AND** no error is thrown
- **AND** the Member termination completes successfully

### Requirement: User Reactivation on Member Reactivation

The system SHALL automatically reactivate the corresponding User account when a Member's membership is reactivated. This ensures restored access for rejoining members.

#### Scenario: User account reactivated when member reactivated

- **GIVEN** a terminated Member with suspended User account
- **WHEN** the Member's membership is reactivated via domain command
- **THEN** the MemberReactivatedEvent is published with registrationNumber
- **AND** the User account accountStatus is set to ACTIVE
- **AND** the User account enabled flag is set to true
- **AND** the User account isAuthenticatable() returns true
- **AND** authentication succeeds with valid credentials

#### Scenario: User reactivation is idempotent

- **GIVEN** an active Member with already active User account
- **WHEN** the MemberReactivatedEvent is processed again
- **THEN** the User account remains ACTIVE
- **AND** no error is thrown

### Requirement: Suspended User Authentication Behavior

The system SHALL deny authentication for suspended users regardless of valid credentials.

#### Scenario: Authentication fails for suspended user

- **GIVEN** a suspended User account (accountStatus=SUSPENDED, enabled=false)
- **WHEN** authentication is attempted with valid username and password
- **THEN** authentication fails
- **AND** HTTP 401 Unauthorized or 403 Forbidden is returned
- **AND** error message indicates account is suspended or disabled

#### Scenario: Existing tokens remain valid until expiration

- **GIVEN** a User account with active access token
- **WHEN** the User account is suspended
- **THEN** existing access token remains valid until expiration
- **AND** new token refresh attempts fail
- **AND** API requests with expired token are rejected

### Requirement: Event-Driven Cross-Module Integration

The system SHALL use domain events for cross-module communication between Members and Users bounded contexts, maintaining architectural boundaries.

#### Scenario: Members module publishes termination event

- **GIVEN** a Member aggregate
- **WHEN** handle(TerminateMembership) command is executed
- **THEN** MemberTerminatedEvent is registered and published
- **AND** event contains memberId, registrationNumber, reason, terminatedAt, terminatedBy
- **AND** event publication is transactional (outbox pattern)

#### Scenario: Users module subscribes to termination event

- **GIVEN** the users module with MemberTerminatedEventHandler
- **WHEN** MemberTerminatedEvent is published
- **THEN** the event handler receives the event
- **AND** User account is looked up by username (registrationNumber)
- **AND** User.suspend() is called
- **AND** updated User is persisted
- **AND** handler runs in separate transaction from Member termination

#### Scenario: Users module does not depend on members module

- **GIVEN** the users module package structure
- **WHEN** reviewing imports in users module
- **THEN** no import from com.klabis.members package exists
- **AND** event handler receives MemberTerminatedEvent via Spring Modulith
- **AND** bounded context isolation is maintained
