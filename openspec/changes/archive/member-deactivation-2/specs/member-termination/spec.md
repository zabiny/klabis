# Delta Spec: member-termination

## MODIFIED Requirements

### Requirement: Membership Termination Request

The system SHALL accept membership termination requests. Termination takes effect immediately upon request and automatically suspends the corresponding User account.

#### Scenario: Valid termination request

- **WHEN** an authenticated user with appropriate permission submits a membership termination request
- **AND** the request contains valid termination reason
- **THEN** the membership is terminated
- **AND** the corresponding User account is suspended
- **AND** the changes are committed atomically

### Requirement: User Suspension on Member Termination

The system SHALL automatically suspend the corresponding User account when a Member is terminated.

#### Scenario: User suspended when member is terminated

- **GIVEN** an active Member with a corresponding active User account
- **WHEN** the Member is terminated
- **THEN** the Member is marked as terminated
- **AND** the corresponding User account is suspended
- **AND** the User cannot authenticate to the system

#### Scenario: Member termination succeeds when User does not exist

- **GIVEN** a Member exists without a corresponding User account
- **WHEN** the Member is terminated
- **THEN** the Member is terminated successfully
- **AND** the operation completes without error

## ADDED Requirements

### Requirement: Member Reactivation

The system SHALL support reactivating terminated memberships, restoring the Member to active state and automatically reactivating the corresponding User account.

#### Scenario: Member reactivation successful

- **GIVEN** a terminated Member
- **WHEN** an authenticated user with appropriate permission submits a reactivation request
- **THEN** the Member is marked as active
- **AND** the corresponding User account is reactivated
- **AND** the User can authenticate to the system
- **AND** the changes are committed atomically

#### Scenario: Reactivation of active member rejected

- **GIVEN** an active Member
- **WHEN** a reactivation request is submitted
- **THEN** the request is rejected
- **AND** an appropriate error message is returned
- **AND** the Member remains active

#### Scenario: User reactivated when member is reactivated

- **GIVEN** a terminated Member with a corresponding suspended User account
- **WHEN** the Member is reactivated
- **THEN** the Member is marked as active
- **AND** the corresponding User account is reactivated
- **AND** the User can authenticate to the system

#### Scenario: Member reactivation succeeds when User does not exist

- **GIVEN** a terminated Member exists without a corresponding User account
- **WHEN** the Member is reactivated
- **THEN** the Member is reactivated successfully
- **AND** the operation completes without error
