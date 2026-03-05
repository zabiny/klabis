## MODIFIED Requirements

### Requirement: Password Setup Token Generation

The system SHALL generate a unique, time-limited password setup token when a new user account is created with
PENDING_ACTIVATION status.

Token specifications:

- Format: UUID v4 (36 characters)
- Uniqueness: Guaranteed across all tokens using cryptographically random generation
- Expiration: 4 hours from generation time (configurable via application.yml)
- Storage: SHA-256 hashed before storage in database
- Single-use: Token marked as used after successful password setup
- Implementation: Password management logic resides in `com.klabis.users` module (not members module)

#### Scenario: Token generated on member registration

- **GIVEN** a new member is being registered
- **WHEN** the member registration completes successfully
- **THEN** a unique password setup token SHALL be generated
- **AND** the token SHALL be hashed using SHA-256 before storage
- **AND** the token expiration SHALL be set to 4 hours from creation
- **AND** the token SHALL be stored in the password_setup_tokens table
- **AND** the token generation SHALL be triggered by `UserCreatedEvent` published when User entity is created
- **AND** the event SHALL be handled by `PasswordSetupEventListener` in users module

#### Scenario: Token uniqueness guaranteed

- **GIVEN** multiple members registering concurrently
- **WHEN** password setup tokens are generated
- **THEN** each token SHALL be unique across all tokens in the system
- **AND** UUID v4 collision probability SHALL be negligible
- **AND** token generation SHALL be handled by `PasswordSetupService` in users module

### Requirement: Password Setup Email Notification

The system SHALL send an email containing a password setup link when a password setup token is generated.

Email specifications:

- Subject: "Set up your Klabis account password"
- Recipient: Member's primary email address
- Template: Configurable via Spring Thymeleaf templates
- Content must include: greeting with member name, setup link with token, expiration warning, support contact
- Implementation: Email sending orchestrated by `PasswordSetupService` in `com.klabis.users.application`

#### Scenario: Email sent after token generation

- **GIVEN** a password setup token has been generated for a new user
- **WHEN** the token generation completes successfully
- **THEN** an email SHALL be sent to the member's primary email address
- **AND** the email SHALL contain a setup link with the plain-text token as query parameter
- **AND** the email SHALL display the token expiration time (4 hours)
- **AND** the email SHALL include support contact information
- **AND** the email sending SHALL be handled by `PasswordSetupService` in users module

#### Scenario: Email sending failure logged

- **GIVEN** a password setup token has been generated
- **WHEN** email sending fails due to SMTP error
- **THEN** the failure SHALL be logged with error details
- **AND** the token SHALL remain valid in the database
- **AND** the user SHALL be able to request a new token via reissuance endpoint
- **AND** the error handling SHALL be performed by `PasswordSetupService` in users module

### Requirement: Password Setup Token Validation

The system SHALL validate password setup tokens before allowing password setup, checking token existence, expiration,
usage status, and user account status.

Validation checks:

1. Token exists in database (compare SHA-256 hash)
2. Token is not expired (created_at + 4 hours > current time)
3. Token is not already used (used_at is NULL)
4. Associated user has PENDING_ACTIVATION status

- Implementation: Token validation performed by `PasswordSetupService` in `com.klabis.users.application`

#### Scenario: Valid token accepted

- **GIVEN** a password setup token exists in the database
- **AND** the token is not expired (within 4 hours of creation)
- **AND** the token has not been used (used_at is NULL)
- **AND** the associated user has PENDING_ACTIVATION status
- **WHEN** token validation is requested via GET /api/auth/password-setup/validate
- **THEN** the system SHALL return 200 OK
- **AND** the response SHALL include {valid: true, email: "partial email", expiresAt: "timestamp"}
- **AND** the validation SHALL be performed by `PasswordSetupController` in users module

#### Scenario: Expired token rejected

- **GIVEN** a password setup token exists in the database
- **AND** the token creation time is more than 4 hours ago
- **WHEN** token validation is requested
- **THEN** the system SHALL return 410 Gone
- **AND** the response SHALL include error message "Token has expired. Please request a new one."

#### Scenario: Used token rejected

- **GIVEN** a password setup token exists in the database
- **AND** the token has been used (used_at is not NULL)
- **WHEN** token validation is requested
- **THEN** the system SHALL return 410 Gone
- **AND** the response SHALL include error message "Token has already been used. Please log in or request password
  reset."

#### Scenario: Invalid token rejected

- **GIVEN** a token value is provided
- **AND** no matching token hash exists in the database
- **WHEN** token validation is requested
- **THEN** the system SHALL return 404 Not Found
- **AND** the response SHALL include error message "Invalid password setup token"

#### Scenario: Account already active rejected

- **GIVEN** a valid password setup token exists
- **AND** the associated user account status is ACTIVE
- **WHEN** token validation is requested
- **THEN** the system SHALL return 400 Bad Request
- **AND** the response SHALL include error message indicating account is already active

### Requirement: Password Setup Completion

The system SHALL allow users to set their password using a valid token, validate password complexity, and activate the
account atomically.

Password complexity requirements:

- Minimum length: 12 characters
- Maximum length: 128 characters
- Must contain: at least one uppercase letter, one lowercase letter, one digit, one special character
- Must NOT contain: user's registration number, first name, or last name

Process steps (atomic transaction):

1. Validate token (all validation checks from FR-3)
2. Validate password meets complexity requirements
3. Hash password using BCrypt (12 rounds)
4. Update user.password_hash
5. Mark token as used (set used_at timestamp and used_by_ip)
6. Change user.account_status to ACTIVE
7. Set user.enabled = true
8. Return success response

- Implementation: Password setup completed by `PasswordSetupService` in `com.klabis.users.application`
- Validation: Password complexity validated by `PasswordComplexityValidator` in `com.klabis.users.domain`

#### Scenario: Password setup successful

- **GIVEN** a valid password setup token
- **AND** a password meeting complexity requirements
- **WHEN** POST /api/auth/password-setup/complete is called with {token, password, passwordConfirmation}
- **THEN** the password SHALL be hashed using BCrypt with 12 rounds
- **AND** the user.password_hash SHALL be updated
- **AND** the token SHALL be marked as used with current timestamp
- **AND** the user.account_status SHALL be changed to ACTIVE
- **AND** the user.enabled SHALL be set to true
- **AND** the system SHALL return 200 OK with {message: "Success", registrationNumber}
- **AND** the user SHALL be able to authenticate immediately
- **AND** the operation SHALL be handled by `PasswordSetupService` in users module

#### Scenario: Password complexity validation failure

- **GIVEN** a valid password setup token
- **AND** a password that does not meet complexity requirements
- **WHEN** POST /api/auth/password-setup/complete is called
- **THEN** the system SHALL return 400 Bad Request
- **AND** the response SHALL include specific validation errors for each failed requirement
- **AND** the token SHALL remain valid and unused
- **AND** the user account SHALL remain PENDING_ACTIVATION
- **AND** the validation SHALL be performed by `PasswordComplexityValidator` in users.domain

#### Scenario: Password confirmation mismatch

- **GIVEN** a valid password setup token
- **AND** password and passwordConfirmation do not match
- **WHEN** POST /api/auth/password-setup/complete is called
- **THEN** the system SHALL return 400 Bad Request
- **AND** the response SHALL include error "Passwords do not match"
- **AND** the token SHALL remain valid and unused

#### Scenario: Password contains personal information

- **GIVEN** a valid password setup token
- **AND** a password containing the user's registration number, first name, or last name
- **WHEN** POST /api/auth/password-setup/complete is called
- **THEN** the system SHALL return 400 Bad Request
- **AND** the response SHALL include error "Password cannot contain personal information"
- **AND** the validation SHALL be performed by `PasswordComplexityValidator` in users.domain
- **AND** the token SHALL remain valid and unused

#### Scenario: Atomicity on failure

- **GIVEN** a valid password setup token
- **AND** a valid password
- **WHEN** POST /api/auth/password-setup/complete is called
- **AND** a database error occurs during the transaction
- **THEN** ALL changes SHALL be rolled back
- **AND** the token SHALL remain valid and unused
- **AND** the user account SHALL remain PENDING_ACTIVATION
- **AND** the system SHALL return 500 Internal Server Error

### Requirement: Password Setup Token Reissuance

The system SHALL allow users with PENDING_ACTIVATION status to request a new password setup token if the previous token
expired, with rate limiting to prevent abuse.

Rate limiting specifications:

- Scope: Per registration number (not per IP address)
- Maximum requests: 3 per hour per registration number
- Minimum delay: 10 minutes between consecutive token requests
- Response: 429 Too Many Requests with Retry-After header

Prerequisites:

- User must exist with PENDING_ACTIVATION status
- Rate limit not exceeded

Process steps:

1. Validate user exists with PENDING_ACTIVATION status
2. Check rate limit (registration number scope)
3. Invalidate all existing tokens for user (set logical delete flag or mark as expired)
4. Generate new token (same process as FR-1)
5. Send email with new token (same process as FR-2)
6. Log token request event for audit

- Implementation: Token reissuance handled by `PasswordSetupService` in `com.klabis.users.application`

#### Scenario: Token reissuance successful

- **GIVEN** a user with PENDING_ACTIVATION status
- **AND** the user has not exceeded rate limits (less than 3 requests in past hour)
- **AND** at least 10 minutes have passed since last token request
- **WHEN** POST /api/auth/password-setup/request is called with {registrationNumber}
- **THEN** all existing tokens for the user SHALL be invalidated
- **AND** a new password setup token SHALL be generated
- **AND** an email with the new token SHALL be sent to the user
- **AND** the token request SHALL be logged with timestamp and IP address
- **AND** the system SHALL return 200 OK with generic success message
- **AND** the operation SHALL be handled by `PasswordSetupService` in users module

#### Scenario: Rate limit exceeded by request count

- **GIVEN** a user with PENDING_ACTIVATION status
- **AND** the user has requested 3 tokens in the past hour
- **WHEN** POST /api/auth/password-setup/request is called
- **THEN** the system SHALL return 429 Too Many Requests
- **AND** the response SHALL include Retry-After header with seconds until next request allowed
- **AND** the response SHALL include error message "You have requested too many tokens"
- **AND** no token SHALL be generated
- **AND** the rate limit attempt SHALL be logged

#### Scenario: Rate limit exceeded by minimum delay

- **GIVEN** a user with PENDING_ACTIVATION status
- **AND** the user requested a token less than 10 minutes ago
- **WHEN** POST /api/auth/password-setup/request is called
- **THEN** the system SHALL return 429 Too Many Requests
- **AND** the response SHALL include Retry-After header with seconds until 10 minutes elapsed
- **AND** no token SHALL be generated

#### Scenario: Account already active

- **GIVEN** a user with ACTIVE status
- **WHEN** POST /api/auth/password-setup/request is called with the user's registrationNumber
- **THEN** the system SHALL return 400 Bad Request
- **AND** the response SHALL include error "Your account is already active. Please log in or use password reset."
- **AND** no token SHALL be generated
- **AND** the request SHALL be logged for security monitoring

#### Scenario: Generic response for unknown registration number

- **GIVEN** a registration number that does not exist in the database
- **WHEN** POST /api/auth/password-setup/request is called
- **THEN** the system SHALL return 200 OK with generic success message
- **AND** no email SHALL be sent
- **AND** the request SHALL be logged with the attempted registration number

### Requirement: Member Registration Integration

The system SHALL integrate password setup token generation into the existing member registration flow, automatically
creating users with PENDING_ACTIVATION status and triggering password setup emails.

Integration specifications:

- Member registration API endpoint remains unchanged (no breaking changes)
- User creation behavior changes from temporary password to pending activation
- Password setup email sent asynchronously via event-driven communication (non-blocking)
- Registration success not dependent on email sending (return 201 even if email fails)
- Event flow: User creation → UserCreatedEvent → PasswordSetupEventListener → token generation + email
- Works for any user creation context (member registration, admin creation, import scripts, etc.)
- No cross-module dependencies needed (both event publisher and listener in users module)

#### Scenario: Member registration creates pending user

- **GIVEN** a member registration request is being processed
- **WHEN** POST /api/members is called with valid member data
- **THEN** a Member entity SHALL be created and persisted in members module
- **AND** a User entity SHALL be created with PENDING_ACTIVATION status in users module
- **AND** the User SHALL have a random temporary password hash (unusable for login)
- **AND** the User enabled flag SHALL be set to false
- **AND** the system SHALL return 201 Created with member resource
- **AND** when User is created, it SHALL register `UserCreatedEvent` via `registerEvent()` method
- **AND** UserEntity SHALL receive the event via `.andEvents()` in UserMapper
- **AND** Spring Data SHALL publish the event automatically after transaction commit

#### Scenario: Token generation and email sending triggered

- **GIVEN** a member registration has created a pending user
- **AND** the User entity has been persisted
- **AND** the transaction has committed successfully
- **WHEN** Spring Data publishes the `UserCreatedEvent` from the event outbox
- **THEN** a password setup token SHALL be generated by `PasswordSetupService` in users module
- **AND** an email with the setup link SHALL be sent to the member's primary email
- **AND** the email sending SHALL be asynchronous and not block the registration response
- **AND** the event SHALL be handled by `PasswordSetupEventListener` within the users module via
  `@ApplicationModuleListener`
- **AND** this pattern works for any user creation (member registration, admin creation, import scripts, etc.)

#### Scenario: Registration succeeds even if email fails

- **GIVEN** a member registration has created a pending user
- **AND** a password setup token has been generated
- **WHEN** the email sending fails due to SMTP error
- **THEN** the registration SHALL still return 201 Created
- **AND** the member and user SHALL be persisted in the database
- **AND** the token SHALL remain valid
- **AND** the email failure SHALL be logged for monitoring
- **AND** the user SHALL be able to request a new token via reissuance endpoint

### Requirement: REST API Endpoints

The system SHALL provide three REST API endpoints for password setup flow: token validation, password setup completion,
and token reissuance.

Endpoints:

1. GET /api/auth/password-setup/validate?token={token}
2. POST /api/auth/password-setup/complete (body: {token, password, passwordConfirmation})
3. POST /api/auth/password-setup/request (body: {registrationNumber})

All endpoints:

- Content-Type: application/json
- Accept: application/json
- Authentication: None (public endpoints)
- CORS: Configured via frontend.allowed-origins
- Implementation: Endpoints defined in `PasswordSetupController` in `com.klabis.users.presentation`

#### Scenario: Validate endpoint returns token status

- **GIVEN** a client application needs to check token validity
- **WHEN** GET /api/auth/password-setup/validate?token={uuid} is called
- **THEN** the system SHALL validate the token
- **AND** return 200 OK with {valid: true, email: "partial", expiresAt: "timestamp"} for valid tokens
- **AND** return appropriate error responses (404, 410, 400) for invalid tokens
- **AND** the endpoint SHALL be handled by `PasswordSetupController` in users module

#### Scenario: Complete endpoint activates account

- **GIVEN** a user has entered their password on the setup page
- **WHEN** POST /api/auth/password-setup/complete is called with valid token and password
- **THEN** the system SHALL complete the password setup process
- **AND** return 200 OK with {message: "Success", registrationNumber}
- **AND** return appropriate error responses (400, 404, 410) for validation failures
- **AND** the endpoint SHALL be handled by `PasswordSetupController` in users module

#### Scenario: Request endpoint issues new token

- **GIVEN** a user needs a new token because previous expired
- **WHEN** POST /api/auth/password-setup/request is called with {registrationNumber}
- **THEN** the system SHALL process the token reissuance request
- **AND** return 200 OK with generic success message (regardless of user existence)
- **AND** return 429 Too Many Requests with Retry-After header if rate limited
- **AND** return 400 Bad Request if account already active
- **AND** the endpoint SHALL be handled by `PasswordSetupController` in users module

#### Scenario: CORS headers included in responses

- **GIVEN** a request from the configured frontend origin
- **WHEN** any password setup endpoint is called
- **THEN** the response SHALL include appropriate CORS headers
- **AND** the frontend SHALL be able to access the response
