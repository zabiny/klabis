# Delta Spec: member-termination

## MODIFIED Requirements

### Requirement: Membership Termination Request

The system SHALL accept membership termination requests with reason and optional note. Termination takes effect immediately upon request and triggers automatic User account suspension via domain event.

#### Scenario: Valid termination request

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits POST request to /api/members/{id}/terminate
- **AND** request contains valid deactivation reason (ODHLASKA, PRESTUP, OTHER)
- **THEN** membership termination is processed immediately
- **AND** HTTP 204 No Content status is returned
- **AND** MemberTerminatedEvent is published with registrationNumber
- **AND** corresponding User account is suspended in separate transaction

### Requirement: Domain Event Publishing

The system SHALL publish a domain event upon successful membership termination for integration with other modules, including automatic User account suspension.

#### Scenario: MemberTerminatedEvent is published

- **WHEN** membership termination is successfully committed to database
- **THEN** MemberTerminatedEvent is published
- **AND** event contains memberId
- **AND** event contains registrationNumber (for User lookup)
- **AND** event contains deactivationReason
- **AND** event contains deactivatedAt timestamp
- **AND** event contains terminatedBy user ID
- **AND** users module subscribes to this event for User suspension

## ADDED Requirements

### Requirement: Member Reactivation

The system SHALL support reactivating terminated memberships, restoring the Member to active state and triggering automatic User account reactivation.

#### Scenario: Member reactivation successful

- **GIVEN** a terminated Member (active=false)
- **WHEN** authenticated user with MEMBERS:UPDATE permission submits reactivation command
- **THEN** Member.active is set to true
- **AND** deactivationReason, deactivatedAt, deactivationNote, deactivatedBy are cleared
- **AND** MemberReactivatedEvent is published with registrationNumber
- **AND** HTTP 204 No Content status is returned

#### Scenario: Reactivation of active member rejected

- **GIVEN** an active Member (active=true)
- **WHEN** reactivation command is submitted
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates member is already active
- **AND** no event is published

#### Scenario: MemberReactivatedEvent triggers User reactivation

- **WHEN** MemberReactivatedEvent is published
- **AND** event contains registrationNumber
- **THEN** users module suspends to this event
- **AND** corresponding User account is reactivated (accountStatus=ACTIVE, enabled=true)
