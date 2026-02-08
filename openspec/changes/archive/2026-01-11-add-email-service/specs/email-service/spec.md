# Email Service Capability

## ADDED Requirements

### Requirement: Welcome Email on Registration

When a new member is successfully registered, the system SHALL send a welcome email to the member's primary email
address (or guardian's email for minors without email).

The system SHALL:

- Send email asynchronously after transaction commits
- Include member's name and registration number in the email
- Include activation link with secure token
- Send email in both HTML and plain-text formats
- Not fail member registration if email delivery fails

#### Scenario: Adult member receives welcome email

- **GIVEN** a user with MEMBERS:CREATE authority
- **WHEN** they register an adult member with email "jan@example.com"
- **THEN** the member is created successfully
- **AND** a welcome email is sent to "jan@example.com"
- **AND** the email contains an activation link that expires in 72 hours

#### Scenario: Minor member welcome email sent to guardian

- **GIVEN** a user with MEMBERS:CREATE authority
- **WHEN** they register a minor member without email
- **AND** the guardian has email "parent@example.com"
- **THEN** the member is created successfully
- **AND** a welcome email is sent to "parent@example.com"

#### Scenario: Member without any email address

- **GIVEN** a user with MEMBERS:CREATE authority
- **WHEN** they register an adult member without email
- **THEN** the member is created successfully
- **AND** no welcome email is sent
- **AND** a warning is logged

### Requirement: Secure Activation Tokens

The system SHALL generate secure, time-limited activation tokens for email-based account activation.

The system SHALL:

- Generate tokens using cryptographically secure random generator (SecureRandom)
- Expire tokens after 72 hours
- Invalidate tokens after single use (activation)
- Return appropriate error messages for expired or invalid tokens

#### Scenario: Successful account activation

- **GIVEN** a member received a welcome email with activation link
- **WHEN** they click the activation link within 72 hours
- **THEN** their account is activated
- **AND** the activation token is invalidated

#### Scenario: Expired activation token

- **GIVEN** a member received a welcome email with activation link
- **WHEN** they click the activation link after 72 hours
- **THEN** they receive an error message "Activation link has expired"
- **AND** the account remains inactive

#### Scenario: Already used activation token

- **GIVEN** a member has already activated their account
- **WHEN** they click the activation link again
- **THEN** they receive an error message "Invalid activation link"

### Requirement: Email Templates

The system SHALL use Thymeleaf templates for rendering email content with both HTML and plain-text versions.

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

Email sending SHALL be triggered by domain events using Spring's event mechanism with transactional safety.

The system SHALL:

- Listen for MemberCreatedEvent via @TransactionalEventListener
- Process events only after successful transaction commit (phase = AFTER_COMMIT)
- Execute email sending asynchronously (@Async)
- Not block the API response

#### Scenario: Email sent after transaction commits

- **GIVEN** a member registration request
- **WHEN** the member is persisted successfully
- **AND** the transaction commits
- **THEN** the MemberCreatedEventHandler is invoked
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

Email settings SHALL be configurable via application properties.

The system SHALL support configuration of:

- SMTP host, port, and credentials
- From email address
- Activation token validity period
- Base URL for activation links

#### Scenario: Custom email configuration

- **GIVEN** custom email properties in application.yml
- **WHEN** the application starts
- **THEN** the EmailService uses the configured SMTP settings
- **AND** emails are sent from the configured from address

### Requirement: Account Activation Endpoint

The system SHALL provide a REST endpoint for account activation via token.

Endpoint: `GET /api/activate?token={token}`

Success response (200 OK):

- Returns JSON with activation confirmation and registration number

Error responses:

- 400 Bad Request for expired tokens (type: activation-token-expired)
- 400 Bad Request for invalid/used tokens (type: activation-token-invalid)

#### Scenario: Successful activation via endpoint

- **GIVEN** a valid, unexpired activation token
- **WHEN** GET /api/activate?token={valid-token}
- **THEN** response status is 200 OK
- **AND** response contains "Account activated successfully"

#### Scenario: Expired token via endpoint

- **GIVEN** an expired activation token
- **WHEN** GET /api/activate?token={expired-token}
- **THEN** response status is 400 Bad Request
- **AND** response type is "activation-token-expired"
