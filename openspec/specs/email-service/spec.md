# email-service Specification

## Purpose

This specification defines requirements for the email service that delivers transactional emails to Klabis members. The
service handles asynchronous email delivery for member registration welcome emails and password setup notifications,
ensuring reliable communication without blocking the main application flow.

## Requirements

### Requirement: Email Templates

The system SHALL use template engine for rendering email content with both HTML and plain-text versions.

The system SHALL:

- Provide HTML template for rich email clients
- Provide plain-text template for text-only clients
- Support variable substitution (firstName, lastName, registrationNumber, activationUrl, clubName)
- Externalize templates (not hardcoded in code)

#### Scenario: Email rendered with correct variables

- **GIVEN** a member "Jan Novák" with registration number "ZBM2501"
- **WHEN** a welcome email is generated
- **THEN** the email body contains "Jan"
- **AND** the email body contains "Novák"
- **AND** the email body contains "ZBM2501"
- **AND** the email body contains a valid activation URL

#### Scenario: Email sent as multipart

- **WHEN** a welcome email is sent
- **THEN** the email contains both HTML and plain-text parts

### Requirement: Event-Driven Email Sending

Email sending SHALL be triggered by domain events using event-driven architecture with transactional safety.

The system SHALL:

- Listen for member created events via transactional event listener
- Process events only after successful transaction commit
- Execute email sending asynchronously
- Not block the API response

#### Scenario: Email sent after transaction commits

- **GIVEN** a member registration request
- **WHEN** the member is persisted successfully
- **AND** the transaction commits
- **THEN** the event handler is invoked
- **AND** the welcome email is sent asynchronously

#### Scenario: No email sent on transaction rollback

- **GIVEN** a member registration request
- **WHEN** the transaction is rolled back due to an error
- **THEN** no welcome email is sent

### Requirement: Graceful Failure Handling

Email failures SHALL NOT affect the member registration process.

The system SHALL:

- Log SMTP failures but not throw exceptions to caller
- Log invalid email addresses and skip sending
- Fall back to plain-text on template rendering errors
- Log all failures with registration number only (no PII)

#### Scenario: SMTP server unavailable

- **GIVEN** a user with MEMBERS:CREATE authority
- **WHEN** they register a member
- **AND** the SMTP server is unavailable
- **THEN** the member is created successfully
- **AND** the email failure is logged
- **AND** no error is returned to the user

#### Scenario: Template rendering error

- **GIVEN** a template rendering error occurs
- **WHEN** attempting to send a welcome email
- **THEN** the system falls back to plain-text email
- **AND** the error is logged

### Requirement: Email Configuration

Email settings SHALL be configurable via system configuration.

The system SHALL support configuration of:

- SMTP host, port, and credentials
- From email address
- Activation token validity period
- Base URL for activation links

#### Scenario: Custom email configuration

- **GIVEN** custom email properties in configuration files
- **WHEN** the application starts
- **THEN** the email service uses the configured SMTP settings
- **AND** emails are sent from the configured from address


