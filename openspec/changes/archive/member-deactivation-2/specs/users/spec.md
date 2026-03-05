# Delta Spec: users

## ADDED Requirements

### Requirement: User Account Suspension

The system SHALL support suspending user accounts. Suspended users cannot authenticate to the system but their accounts persist in the database.

#### Scenario: User account suspended successfully

- **GIVEN** an active User account
- **WHEN** the system suspends the User account
- **THEN** the User account is marked as suspended
- **AND** the User cannot authenticate to the system
- **AND** the User account remains in the database

#### Scenario: Suspended user cannot authenticate

- **GIVEN** a suspended User account
- **WHEN** the User attempts to authenticate with valid credentials
- **THEN** authentication fails
- **AND** no access token is issued

#### Scenario: Suspend operation is idempotent

- **GIVEN** a User account that is already suspended
- **WHEN** the system attempts to suspend the User account again
- **THEN** the operation completes successfully
- **AND** no error occurs

### Requirement: User Account Reactivation

The system SHALL support reactivating suspended user accounts, restoring their ability to authenticate.

#### Scenario: User account reactivated successfully

- **GIVEN** a suspended User account
- **WHEN** the system reactivates the User account
- **THEN** the User account is marked as active
- **AND** the User can authenticate with valid credentials
- **AND** the User account remains in the database

#### Scenario: Reactivate operation is idempotent

- **GIVEN** a User account that is already active
- **WHEN** the system attempts to reactivate the User account again
- **THEN** the operation completes successfully
- **AND** no error occurs

### Requirement: Graceful Handling of Missing User

The system SHALL gracefully handle cases where a User account does not exist when suspension or reactivation is requested.

#### Scenario: Suspend non-existent user

- **GIVEN** no User account exists
- **WHEN** the system attempts to suspend the User account
- **THEN** the operation completes successfully
- **AND** no error occurs

#### Scenario: Reactivate non-existent user

- **GIVEN** no User account exists
- **WHEN** the system attempts to reactivate the User account
- **THEN** the operation completes successfully
- **AND** no error occurs
