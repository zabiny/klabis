## ADDED Requirements

### Requirement: Password setup email for new user accounts

The system SHALL send password setup email when a new user account is created with email address during member
registration.

#### Scenario: Send password setup email during member registration

- **WHEN** a new member is registered with a valid email address
- **THEN** the system SHALL create a user account for the member
- **AND** the system SHALL send a password setup email to the provided email address
- **AND** the email SHALL address the user by their registration number (username)

#### Scenario: Do not send password setup email when no email provided

- **WHEN** a user account is created without an email address
- **THEN** the system SHALL create the user account
- **AND** the system SHALL NOT send a password setup email
