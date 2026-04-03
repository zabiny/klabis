## MODIFIED Requirements

### Requirement: Email Templates

The system SHALL use a template engine to render email content in both HTML and plain-text versions. Templates support variable substitution (member name, registration number, activation URL, club name).

#### Scenario: Welcome email contains correct member information

- **WHEN** a welcome email is generated for member "Jan Novák" with registration number "ZBM2501"
- **THEN** the email body includes the member's first name, last name, and registration number
- **AND** the email includes a valid activation URL

#### Scenario: Welcome email contains both HTML and plain-text parts

- **WHEN** a welcome email is sent
- **THEN** the email contains both an HTML version (for rich email clients) and a plain-text version (for text-only clients)

### Requirement: Event-Driven Email Sending

Email sending SHALL be triggered by domain events after the transaction commits. Email sending is asynchronous and does not block the API response.

#### Scenario: Email is sent after successful member registration

- **WHEN** a new member is registered and the transaction commits
- **THEN** the welcome email is sent asynchronously
- **AND** the registration response is returned immediately without waiting for email delivery

#### Scenario: Email is not sent when registration transaction rolls back

- **WHEN** the member registration transaction is rolled back due to an error
- **THEN** no welcome email is sent

### Requirement: Graceful Failure Handling

Email failures SHALL NOT cause the member registration to fail. Failures are logged without exposing PII.

#### Scenario: SMTP unavailable does not fail the registration

- **WHEN** admin registers a member
- **AND** the SMTP server is unavailable
- **THEN** the member is created successfully
- **AND** the email failure is logged
- **AND** no error is shown to the user

#### Scenario: Template rendering error falls back to plain text

- **WHEN** an HTML template rendering error occurs during email generation
- **THEN** the system sends the plain-text version instead
- **AND** the error is logged

### Requirement: Email Configuration

Email settings SHALL be configurable via system configuration (SMTP host, port, credentials, from address, activation token validity, base URL for activation links).

#### Scenario: Email service uses configured SMTP settings

- **WHEN** the application starts with custom email configuration
- **THEN** the email service uses the configured SMTP host, port, and credentials
- **AND** emails are sent from the configured from address
