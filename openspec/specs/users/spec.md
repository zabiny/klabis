# users Specification

## Purpose

This specification defines requirements for user account management including:

- OAuth2 authentication and authorization
- User aggregate management and lifecycle
- User creation with optional email and flexible PII parameters
- User activation through token-based password setup flow
- Password setup email triggered during member registration
- User permissions management as a separate aggregate from user identity
- Direct authority assignment/revocation with admin lockout prevention
- Authorization context (actor, resource owner, required authority)
- Secure token generation and validation
- Permission change audit trail
- Role-based access control
- Token lifecycle management
- Password complexity validation

When new users are created with PENDING_ACTIVATION status, they receive a time-limited, single-use token via email that
allows them to set their initial password and activate their account. This ensures that only users with access to the
registered email address can activate accounts. The permissions model separates authorization from authentication,
enabling contextual checks and future group-based delegated authorization.

## Requirements

### Requirement: User Authentication

The system SHALL authenticate users via OAuth2 using registrationNumber as username and cryptographically hashed
password. **The access token SHALL contain authorities claims from UserPermissions aggregate, not from User entity.**

**Changed from previous version**: Authorities are now loaded from UserPermissions aggregate instead of User entity
during authentication.

#### Scenario: Successful authentication with valid credentials

- **WHEN** user submits valid registrationNumber and password to /oauth2/token
- **THEN** system returns access token (15 min TTL) and refresh token (30 day TTL)
- **AND** access token contains claims: registrationNumber, authorities (from UserPermissions), expiration

#### Scenario: Authentication fails with invalid credentials

- **WHEN** user submits invalid registrationNumber or password
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response includes error description "Invalid credentials"

#### Scenario: Token validation extracts user context

- **WHEN** API receives request with valid access token
- **THEN** system extracts registrationNumber and authorities from token claims
- **AND** SecurityContext is populated with authenticated user
- **AND** authorities are loaded from UserPermissions aggregate (not User entity)

#### Scenario: Expired access token rejected

- **WHEN** API receives request with expired access token
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response indicates token has expired

### Requirement: User Authorization

The system SHALL authorize API operations based on user authorities and OAuth2 scopes. **Authorization checks SHALL
query UserPermissions aggregate to determine user's direct authorities, and MAY consider group-based authorities in the
future.**

**Changed from previous version**: Authorization checks now query UserPermissions aggregate instead of User entity.
Authorization is now context-aware (actor, resourceOwner, requiredAuthority).

#### Scenario: User with required authority accesses endpoint

- **WHEN** authenticated user with MEMBERS:CREATE authority calls POST /api/members
- **THEN** authorization check passes (authority from UserPermissions)
- **AND** endpoint handler executes

#### Scenario: User without required authority denied access

- **WHEN** authenticated user without MEMBERS:CREATE authority calls POST /api/members
- **THEN** authorization check fails
- **AND** system returns HTTP 403 Forbidden with ProblemDetail

#### Scenario: Unauthenticated request denied

- **WHEN** unauthenticated request (no token) is made to secured endpoint
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response includes WWW-Authenticate header with Bearer challenge

---

### Requirement: Authentication Principal

The system SHALL use the user's UUID as the authentication principal name for resolving member associations.

#### Scenario: Authentication principal contains user UUID

- **GIVEN** a user with ID "12345678-1234-1234-1234-123456789012"
- **AND** user successfully authenticates
- **THEN** SecurityContext authentication.getName() returns the user's UUID string
- **AND** this UUID can be used to resolve associated member records

#### Scenario: Username resolution

- **GIVEN** a User entity with userName "john.doe"
- **AND** UUID "12345678-1234-1234-1234-123456789012"
- **WHEN** authentication occurs
- **THEN** the authentication principal (username) is the UUID string, NOT "john.doe"
- **AND** "john.doe" is stored only in User.userName attribute for display purposes

**RATIONALE:** Using UUID as the authentication principal allows direct mapping to UserId and MemberId without
additional lookups. The userName field is kept for display/UI purposes only.

---

### Requirement: Valid Authorities

The system SHALL restrict permission updates to a predefined set of valid authorities mapped to the current
authorization model.

#### Scenario: Valid authorities list

- **GIVEN** the system defines these valid authorities:
    - MEMBERS:CREATE
    - MEMBERS:READ
    - MEMBERS:UPDATE
    - MEMBERS:DELETE
    - MEMBERS:PERMISSIONS
    - EVENTS:MANAGE
- **WHEN** user requests to set permissions with any combination of these authorities
- **THEN** validation passes

#### Scenario: Authority validation error response

- **WHEN** request includes invalid authority
- **THEN** error response includes complete list of valid authorities
- **AND** error message clearly identifies which authority was invalid

---

### Requirement: User Aggregate

The User aggregate SHALL manage user accounts as a separate aggregate from members, linked via registrationNumber. When
a Member is created for a User, the User's UserId SHALL be used as the Member's UserId, establishing a direct identifier
relationship between the aggregates. Users cannot be deleted, only disabled via accountStatus changes. **The User
aggregate SHALL contain only identity-related data (credentials, account status) and SHALL NOT contain authorities or
permissions.**

**Rationale**: Authorities are now managed in separate UserPermissions aggregate to enable separation of authentication
and authorization concerns.

**Changed from previous version**: Removed authorities from User aggregate structure. Users are now created without
authorities - authorities are managed separately via UserPermissions aggregate.

#### Scenario: User created with credentials only

- **WHEN** a user is created with registrationNumber and password
- **THEN** the User aggregate is created with cryptographically hashed password
- **AND** the User has unique UserId (generated as new UUID)
- **AND** User accountStatus is set to specified value
- **AND** User can exist without linked Member
- **AND** User does NOT have authorities field (authorities managed separately)
- **AND** UserPermissions aggregate is created separately (may be empty initially)

#### Scenario: User password changed

- **WHEN** user password is changed with new password
- **THEN** password is cryptographically hashed and stored
- **AND** User identity (registrationNumber) and UserId are preserved
- **AND** linked Member (if exists) is not affected

#### Scenario: User account suspended

- **WHEN** user account is suspended
- **THEN** accountStatus changed to SUSPENDED
- **AND** subsequent authentication attempts fail
- **AND** existing tokens remain valid until expiration
- **AND** linked Member (if exists) remains in database but user cannot access it

#### Scenario: Member created for user uses user UserId

- **WHEN** Member is created for an existing User
- **THEN** Member.id SHALL equal User.id (same UserId value)
- **AND** Member.registrationNumber SHALL equal User.registrationNumber
- **AND** User and Member remain separate aggregates with shared identifier

---

### Requirement: Token Lifecycle

The system SHALL issue access tokens (15 min TTL) and refresh tokens (30 day TTL).

#### Scenario: Access token expires and client uses refresh token

- **WHEN** access token expires and client submits refresh token to /oauth2/token
- **THEN** system validates refresh token in database
- **AND** new access token issued with fresh expiration
- **AND** refresh token reused or rotated (based on configuration)

#### Scenario: Refresh token valid until expiration or revocation

- **WHEN** refresh token is within 30 day TTL and not revoked
- **THEN** refresh token can be used to obtain new access token

#### Scenario: Refresh token revoked

- **WHEN** refresh token is revoked (user logout, password change, admin action)
- **THEN** refresh token marked as revoked in database
- **AND** subsequent refresh attempts fail with HTTP 401

#### Scenario: Client requests token with OAuth2 scopes

- **WHEN** OAuth2 client requests token with scope "members.write"
- **THEN** access token includes scope claim
- **AND** scope mapped to authorities (members.write → MEMBERS:CREATE, MEMBERS:UPDATE, MEMBERS:DELETE)
- **AND** endpoint authorization uses authorities from both roles and scopes

### Requirement: OAuth2 Client Registration

The system SHALL register OAuth2 clients for frontend applications to obtain tokens.

#### Scenario: Bootstrap OAuth2 client for web application

- **WHEN** the system is initialized
- **THEN** OAuth2 client "klabis-web" registered with client_id and client_secret
- **AND** client configured with grant types: authorization_code, refresh_token, client_credentials
- **AND** client configured with scopes: openid, members.read, members.write
- **AND** client configured with redirect URIs for authorization code flow

#### Scenario: Client authenticates with credentials

- **WHEN** client submits client_id and client_secret to /oauth2/token
- **THEN** system validates client credentials
- **AND** client authorized to obtain tokens on behalf of users

---

### Requirement: OpenID Connect ID Token Authentication

The system SHALL support OpenID Connect ID token generation when clients request authentication with the `openid` scope.
ID tokens provide user identity verification (authentication) separate from access tokens (authorization).

When a client includes `openid` in the requested scope during OAuth2 authorization:

- The system SHALL generate an ID token in addition to access and refresh tokens
- The ID token SHALL be a signed JWT containing standard OIDC claims (iss, sub, aud, exp, iat, auth_time)
- The ID token `sub` claim SHALL contain the user's registrationNumber
- The ID token MAY include custom claims (registrationNumber)
- The ID token SHALL be stored in the `oauth2_authorization` table for record-keeping
- The ID token SHALL NOT include firstName or lastName (these are available via UserInfo endpoint)

ID tokens are OPTIONAL - clients that do not request `openid` scope continue to receive only access and refresh tokens (
existing OAuth2 behavior).

#### Scenario: User authentication with openid scope returns ID token

- **GIVEN** user submits valid registrationNumber and password to `/oauth2/authorize` with `scope=openid members.read`
- **AND** user successfully authenticates
- **WHEN** client exchanges authorization code at `/oauth2/token`
- **THEN** system returns access token (15 min TTL), refresh token (30 day TTL), and ID token
- **AND** access token contains claims: registrationNumber, authorities (from UserPermissions), expiration
- **AND** ID token contains claims: iss, sub (registrationNumber), aud, exp, iat, auth_time
- **AND** ID token includes `registrationNumber` claim
- **AND** ID token does NOT include firstName or lastName (available via UserInfo endpoint)
- **AND** all tokens are signed with RS256 algorithm

#### Scenario: User authentication without openid scope returns only access tokens

- **GIVEN** user submits valid registrationNumber and password to `/oauth2/authorize` with `scope=members.read` (no
  `openid`)
- **AND** user successfully authenticates
- **WHEN** client exchanges authorization code at `/oauth2/token`
- **THEN** system returns access token and refresh token only
- **AND** no ID token is generated or returned
- **AND** behavior matches existing OAuth2 flow (backward compatible)

---

### Requirement: OpenID Connect UserInfo Access

The system SHALL provide access to user profile information via the UserInfo endpoint when clients authenticate with the
`openid` scope.

The UserInfo endpoint at `/oauth2/userinfo` SHALL:

- Accept access tokens with `openid` scope
- Return user profile claims (sub, registrationNumber, firstName, lastName)
- Require authentication via valid access token
- Return HTTP 403 Forbidden for access tokens without `openid` scope

This requirement EXTENDS the existing User Authentication and User Authorization requirements by adding a new mechanism
for accessing user profile data.

#### Scenario: Access UserInfo endpoint with openid scope

- **GIVEN** user has valid access token with `openid` scope
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with user profile claims
- **AND** response includes `sub` (registrationNumber)
- **AND** response includes `registrationNumber` claim
- **AND** response includes `firstName` claim (from linked Member entity)
- **AND** response includes `lastName` claim (from linked Member entity)

#### Scenario: UserInfo endpoint rejects request without openid scope

- **GIVEN** user has valid access token without `openid` scope
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 403 Forbidden
- **AND** response indicates insufficient scope (missing `openid`)

---

### Requirement: OpenID Connect Logout

The system SHALL support OIDC RP-initiated logout at `/oauth2/logout` for terminating user sessions. The logout endpoint
provides single sign-out capability for OIDC clients.

The logout endpoint SHALL:

- Accept POST requests with logout parameters
- Validate `post_logout_redirect_uri` against registered client URIs
- Accept optional `state` parameter to maintain session state
- Terminate user session and invalidate refresh tokens
- Redirect to `post_logout_redirect_uri` after successful logout

This requirement EXTENDS the existing Token Lifecycle requirement by adding a logout mechanism for OIDC clients.
Existing OAuth2 token revocation (password change, admin action) continues to work as before.

#### Scenario: User logout via OIDC endpoint

- **GIVEN** user has active session with valid tokens
- **AND** client has registered `post_logout_redirect_uri`
- **WHEN** client sends POST request to `/oauth2/logout` with valid `post_logout_redirect_uri` and `state`
- **THEN** system terminates user session and invalidates refresh tokens
- **AND** system redirects to `post_logout_redirect_uri` with `state` parameter
- **AND** subsequent token refresh attempts fail

#### Scenario: Logout validates redirect URI

- **GIVEN** client has registered `post_logout_redirect_uri=https://client.com/callback`
- **WHEN** client sends POST request to `/oauth2/logout` with `post_logout_redirect_uri=https://evil.com`
- **THEN** system rejects logout request with HTTP 400 Bad Request
- **AND** user session remains active
- **AND** response indicates invalid redirect URI

---

### Requirement: OpenID Connect Discovery

The system SHALL provide OIDC provider configuration at `/.well-known/openid-configuration` for client discovery. The
discovery endpoint returns metadata about the authorization server's OIDC capabilities.

The discovery endpoint SHALL:

- Be publicly accessible without authentication
- Return JSON metadata including issuer, endpoints, supported scopes, response types, and signing algorithms
- Include `openid` in `scopes_supported`

This requirement ADDS a new discovery endpoint for OIDC clients. OAuth2 clients that do not require OIDC can continue to
use manual configuration.

#### Scenario: Client discovers OIDC provider configuration

- **WHEN** client sends GET request to `/.well-known/openid-configuration`
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `issuer`, `authorization_endpoint`, `token_endpoint`, `jwks_uri`, `userinfo_endpoint`,
  `end_session_endpoint`
- **AND** response includes `scopes_supported` containing `openid`
- **AND** response includes `response_types_supported` and `id_token_signing_alg_values_supported`

---

### Requirement: Bootstrap Admin User

The system SHALL provision an admin user with full permissions upon database initialization.

#### Scenario: Admin user created in database migration

- **WHEN** the system is initialized
- **THEN** user created with registrationNumber "admin"
- **AND** user password is cryptographically hashed from "admin123"
- **AND** user assigned ROLE_ADMIN
- **AND** user accountStatus is ACTIVE
- **AND** user has no linked Member (registrationNumber not in members table)

#### Scenario: Admin user authenticates and creates first member

- **WHEN** admin user authenticates with admin / admin123
- **THEN** access token issued with MEMBERS:CREATE authority
- **AND** admin can call POST /api/members to create first member
- **AND** created member can have different registrationNumber than admin user

### Requirement: Audit Trail with Authentication Context

The system SHALL record authenticated user's registrationNumber as auditor for all data modifications.

#### Scenario: Authenticated user creates resource

- **WHEN** authenticated user with registrationNumber admin creates member
- **THEN** created_by field populated with "admin"
- **AND** created_at field populated with current timestamp

#### Scenario: System operation uses fallback auditor

- **WHEN** unauthenticated operation executes (database migration, scheduled job)
- **THEN** created_by field populated with "system"

#### Scenario: User updates resource

- **WHEN** authenticated user with registrationNumber ZBM0102 updates member
- **THEN** modified_by field populated with "ZBM0102"
- **AND** modified_at field populated with current timestamp

### Requirement: Security Error Handling

The system SHALL return standardized error responses for authentication and authorization failures.

#### Scenario: Unauthorized access returns 401 with ProblemDetail

- **WHEN** unauthenticated request made to secured endpoint
- **THEN** HTTP 401 Unauthorized returned
- **AND** response body is ProblemDetail JSON (RFC 7807)
- **AND** ProblemDetail includes type, title, status, detail fields
- **AND** WWW-Authenticate header included with Bearer challenge

#### Scenario: Forbidden access returns 403 with ProblemDetail

- **WHEN** authenticated user without required authority attempts operation
- **THEN** HTTP 403 Forbidden returned
- **AND** response body is ProblemDetail JSON
- **AND** detail indicates missing authority

#### Scenario: CORS preflight requests allowed

- **WHEN** frontend (http://localhost:3000) sends OPTIONS preflight request
- **THEN** CORS headers included in response
- **AND** Access-Control-Allow-Origin header includes frontend origin
- **AND** Access-Control-Allow-Credentials: true
- **AND** OPTIONS request does not require authentication

### Requirement: Password Complexity Validation

The system SHALL enforce password complexity requirements to ensure user account security.

Password complexity requirements:

- Minimum length: 12 characters
- Maximum length: 128 characters
- Must contain: at least one uppercase letter, one lowercase letter, one digit, one special character
- Must NOT contain: user's registration number, first name, or last name

#### Scenario: Valid password accepted

- **GIVEN** a user is setting or changing their password
- **AND** the password meets all complexity requirements
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL pass
- **AND** the password SHALL be accepted

#### Scenario: Password too short rejected

- **GIVEN** a user is setting their password
- **AND** the password is less than 12 characters
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate minimum length requirement

#### Scenario: Password too long rejected

- **GIVEN** a user is setting their password
- **AND** the password exceeds 128 characters
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate maximum length requirement

#### Scenario: Password missing uppercase rejected

- **GIVEN** a user is setting their password
- **AND** the password contains no uppercase letters
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate uppercase letter requirement

#### Scenario: Password missing lowercase rejected

- **GIVEN** a user is setting their password
- **AND** the password contains no lowercase letters
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate lowercase letter requirement

#### Scenario: Password missing digit rejected

- **GIVEN** a user is setting their password
- **AND** the password contains no digits
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate digit requirement

#### Scenario: Password missing special character rejected

- **GIVEN** a user is setting their password
- **AND** the password contains no special characters
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate special character requirement

#### Scenario: Password contains registration number rejected

- **GIVEN** a user is setting their password
- **AND** the password contains the user's registration number
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate password cannot contain personal information
- **AND** the validation checks against user's registration number from User entity

#### Scenario: Password contains first name rejected

- **GIVEN** a user is setting their password
- **AND** the password contains the user's first name (from linked Member entity)
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate password cannot contain personal information
- **AND** the validation accesses member data via registration number lookup

#### Scenario: Password contains last name rejected

- **GIVEN** a user is setting their password
- **AND** the password contains the user's last name (from linked Member entity)
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** an error message SHALL indicate password cannot contain personal information
- **AND** the validation accesses member data via registration number lookup

#### Scenario: Multiple validation errors reported

- **GIVEN** a user is setting their password
- **AND** the password fails multiple complexity requirements
- **WHEN** the password is submitted for validation
- **THEN** the validation SHALL fail
- **AND** error messages SHALL be provided for ALL failed requirements
- **AND** the user SHALL see all issues in a single response

### Requirement: UserId Value Object

The system SHALL use unique identifiers (UserId) for User and Member aggregates. The identifier wraps a UUID and
provides type safety to prevent accidental mixing of IDs from different aggregates. The identifier is immutable.

#### Scenario: Unique identifier wraps UUID value

- **WHEN** a UserId is created with a valid UUID
- **THEN** the identifier is successfully created
- **AND** the identifier exposes the UUID value
- **AND** the identifier provides equality and hashing based on the wrapped UUID
- **AND** the identifier is immutable

#### Scenario: Unique identifier prevents null UUID

- **WHEN** a UserId is attempted to be created with null UUID
- **THEN** validation fails with error
- **AND** error message indicates UUID cannot be null

#### Scenario: Unique identifier generation from string

- **WHEN** a UserId is created from a valid UUID string
- **THEN** the identifier is created from the parsed UUID
- **AND** if string is invalid UUID format, validation fails with error

### Requirement: Password Setup Token Generation

The system SHALL generate a unique, time-limited password setup token when a new user account is created with
PENDING_ACTIVATION status.

Token specifications:

- Format: UUID v4 (36 characters)
- Uniqueness: Guaranteed across all tokens using cryptographically random generation
- Expiration: 4 hours from generation time (configurable by system administrator)
- Storage: Cryptographically hashed before storage in database
- Single-use: Token marked as used after successful password setup

#### Scenario: Token generated on member registration

- **GIVEN** a new member is being registered
- **WHEN** the member registration completes successfully
- **THEN** a unique password setup token SHALL be generated
- **AND** the token SHALL be cryptographically hashed before storage
- **AND** the token expiration SHALL be set to 4 hours from creation
- **AND** the token SHALL be stored persistently
- **AND** the token generation SHALL be triggered when User entity is created

#### Scenario: Token uniqueness guaranteed

- **GIVEN** multiple members registering concurrently
- **WHEN** password setup tokens are generated
- **THEN** each token SHALL be unique across all tokens in the system
- **AND** UUID v4 collision probability SHALL be negligible
- **AND** token generation SHALL be handled by the system

### Requirement: Password Setup Email Notification

The system SHALL send an email containing a password setup link when a password setup token is generated.

Email specifications:

- Subject: "Set up your Klabis account password"
- Recipient: Member's primary email address
- Template: Configurable email templates
- Content must include: greeting with member name, setup link with token, expiration warning, support contact

#### Scenario: Email sent after token generation

- **GIVEN** a password setup token has been generated for a new user
- **WHEN** the token generation completes successfully
- **THEN** an email SHALL be sent to the member's primary email address
- **AND** the email SHALL contain a setup link with the plain-text token as query parameter
- **AND** the email SHALL display the token expiration time (4 hours)
- **AND** the email SHALL include support contact information
- **AND** the email sending SHALL be handled by the system

#### Scenario: Email sending failure logged

- **GIVEN** a password setup token has been generated
- **WHEN** email sending fails due to SMTP error
- **THEN** the failure SHALL be logged with error details
- **AND** the token SHALL remain valid in the database
- **AND** the user SHALL be able to request a new token via reissuance endpoint
- **AND** the error handling SHALL be performed by the system

### Requirement: Password Setup Token Validation

The system SHALL validate password setup tokens before allowing password setup, checking token existence, expiration,
usage status, and user account status.

Validation checks:

1. Token exists in database (compare SHA-256 hash)
2. Token is not expired (created_at + 4 hours > current time)
3. Token is not already used (used_at is NULL)
4. Associated user has PENDING_ACTIVATION status

#### Scenario: Valid token accepted

- **GIVEN** a password setup token exists in the database
- **AND** the token is not expired (within 4 hours of creation)
- **AND** the token has not been used (used_at is NULL)
- **AND** the associated user has PENDING_ACTIVATION status
- **WHEN** token validation is requested via GET /api/auth/password-setup/validate
- **THEN** the system SHALL return 200 OK
- **AND** the response SHALL include {valid: true, email: "partial email", expiresAt: "timestamp"}
- **AND** the validation SHALL be performed by the system

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

1. Validate token
2. Validate password meets complexity requirements
3. Hash password using cryptographic hashing
4. Update user.password_hash
5. Mark token as used (set used_at timestamp and used_by_ip)
6. Change user.account_status to ACTIVE
7. Set user.enabled = true
8. Return success response

#### Scenario: Password setup successful

- **GIVEN** a valid password setup token
- **AND** a password meeting complexity requirements
- **WHEN** POST /api/auth/password-setup/complete is called with {token, password, passwordConfirmation}
- **THEN** the password SHALL be cryptographically hashed
- **AND** the user.password_hash SHALL be updated
- **AND** the token SHALL be marked as used with current timestamp
- **AND** the user.account_status SHALL be changed to ACTIVE
- **AND** the user.enabled SHALL be set to true
- **AND** the system SHALL return 200 OK with {message: "Success", registrationNumber}
- **AND** the user SHALL be able to authenticate immediately
- **AND** the operation SHALL be handled by the system

#### Scenario: Password complexity validation failure

- **GIVEN** a valid password setup token
- **AND** a password that does not meet complexity requirements
- **WHEN** POST /api/auth/password-setup/complete is called
- **THEN** the system SHALL return 400 Bad Request
- **AND** the response SHALL include specific validation errors for each failed requirement
- **AND** the token SHALL remain valid and unused
- **AND** the user account SHALL remain PENDING_ACTIVATION
- **AND** the validation SHALL be performed by the system

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
- **AND** the validation SHALL be performed by the system
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
4. Generate new token
5. Send email with new token
6. Log token request event for audit

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
- **AND** the operation SHALL be handled by the system

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

### Requirement: Token Security and Storage

The system SHALL store password setup tokens securely using cryptographic hashing and implement proper token comparison
to prevent timing attacks.

Security specifications:

- Hash algorithm: Cryptographically secure hash
- Token comparison: Constant-time comparison
- Plain-text tokens: Never stored in database
- Token transmission: Only via HTTPS in production

#### Scenario: Token hashed before storage

- **GIVEN** a new password setup token is generated
- **WHEN** the token is being stored in the database
- **THEN** the token SHALL be cryptographically hashed
- **AND** only the hash SHALL be stored
- **AND** the plain-text token SHALL never be stored
- **AND** the plain-text token SHALL be returned for email sending only

#### Scenario: Token comparison uses constant-time algorithm

- **GIVEN** a token validation request with a provided token
- **WHEN** the system compares the provided token hash with stored token hash
- **THEN** the comparison SHALL use constant-time comparison algorithm
- **AND** the comparison time SHALL not leak information about token validity

#### Scenario: Token cleanup removes expired tokens

- **GIVEN** password setup tokens in the database
- **AND** some tokens have expired (created_at + 4 hours < current time)
- **WHEN** the scheduled cleanup job runs (daily at midnight)
- **THEN** all expired tokens SHALL be deleted from the database
- **AND** the cleanup operation SHALL be logged with count of deleted tokens

### Requirement: Audit Logging for Token Events

The system SHALL log all password setup token events to application logs for security audit and monitoring.

Log events:

- Token created: timestamp, user_id
- Token used: timestamp, user_id, ip_address
- Token validation failed: timestamp, reason, ip_address
- Token requested: timestamp, user_id, ip_address
- Token cleanup: timestamp, count of deleted tokens

Log format: Structured log entries with consistent format for parsing

#### Scenario: Token creation logged

- **GIVEN** a password setup token is being generated
- **WHEN** the token is successfully created
- **THEN** an audit log entry SHALL be written to application log
- **AND** the log SHALL include: event_type="token_created", timestamp, user_id, token_id

#### Scenario: Token usage logged

- **GIVEN** a user is setting their password with a valid token
- **WHEN** the password setup completes successfully
- **THEN** an audit log entry SHALL be written to application log
- **AND** the log SHALL include: event_type="token_used", timestamp, user_id, token_id, ip_address

#### Scenario: Failed validation logged

- **GIVEN** a token validation request with an invalid token
- **WHEN** the validation fails
- **THEN** an audit log entry SHALL be written to application log
- **AND** the log SHALL include: event_type="validation_failed", timestamp, reason, ip_address, attempted_token_hash

#### Scenario: Token request logged

- **GIVEN** a user is requesting a new password setup token
- **WHEN** the request is processed (regardless of success or rate limit)
- **THEN** an audit log entry SHALL be written to application log
- **AND** the log SHALL include: event_type="token_requested", timestamp, user_id, ip_address, success_status

#### Scenario: Cleanup job logged

- **GIVEN** the scheduled token cleanup job is running
- **WHEN** expired tokens are deleted
- **THEN** an audit log entry SHALL be written to application log
- **AND** the log SHALL include: event_type="token_cleanup", timestamp, count_deleted

### Requirement: Member Registration Integration

The system SHALL integrate password setup token generation into the existing member registration flow, automatically
creating users with PENDING_ACTIVATION status and triggering password setup emails.

Integration specifications:

- Member registration API endpoint remains unchanged (no breaking changes)
- User creation behavior changes from temporary password to pending activation
- Password setup email sent asynchronously (non-blocking)
- Registration success not dependent on email sending (return 201 even if email fails)
- Works for any user creation context (member registration, admin creation, import scripts, etc.)

#### Scenario: Member registration creates pending user

- **GIVEN** a member registration request is being processed
- **WHEN** POST /api/members is called with valid member data
- **THEN** a Member entity SHALL be created and persisted
- **AND** a User entity SHALL be created with PENDING_ACTIVATION status
- **AND** the User SHALL have a random temporary password hash (unusable for login)
- **AND** the User enabled flag SHALL be set to false
- **AND** the system SHALL return 201 Created with member resource
- **AND** when User is created, password setup flow SHALL be triggered

#### Scenario: Token generation and email sending triggered

- **GIVEN** a member registration has created a pending user
- **AND** the User entity has been persisted
- **AND** the transaction has committed successfully
- **WHEN** user creation is complete
- **THEN** a password setup token SHALL be generated
- **AND** an email with the setup link SHALL be sent to the member's primary email
- **AND** the email sending SHALL be asynchronous and not block the registration response
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

### Requirement: Configuration Management

The system SHALL provide configurable settings for password setup token behavior.

Configuration properties:

- Token expiration duration (default: 4 hours)
- Token hash algorithm (default: cryptographically secure hash)
- Rate limit requests per hour (default: 3)
- Rate limit minimum delay between requests (default: 10 minutes)
- Email template name
- Application base URL for setup links
- SMTP settings (host, port, username, password, from address)

#### Scenario: Token expiration configurable

- **GIVEN** the system configuration contains token expiration setting
- **WHEN** a password setup token is generated
- **THEN** the token expiration SHALL be set to configured hours from creation time
- **AND** the default SHALL be 4 hours if not configured

#### Scenario: Rate limiting configurable

- **GIVEN** the system configuration contains rate limit settings
- **WHEN** token reissuance requests are evaluated
- **THEN** the rate limit SHALL use the configured requests-per-hour value
- **AND** the minimum delay SHALL use the configured min-delay-between-requests value
- **AND** the defaults SHALL be 3 requests/hour and 10 minutes if not configured

#### Scenario: Email settings configurable

- **GIVEN** the system configuration contains email settings
- **WHEN** password setup emails are sent
- **THEN** the email service SHALL use the configured SMTP host, port, and credentials
- **AND** the email SHALL be sent from the configured from address
- **AND** the setup link SHALL use the configured base URL

### Requirement: REST API Endpoints for Password Setup

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

#### Scenario: Validate endpoint returns token status

- **GIVEN** a client application needs to check token validity
- **WHEN** GET /api/auth/password-setup/validate?token={uuid} is called
- **THEN** the system SHALL validate the token
- **AND** return 200 OK with {valid: true, email: "partial", expiresAt: "timestamp"} for valid tokens
- **AND** return appropriate error responses (404, 410, 400) for invalid tokens
- **AND** the endpoint SHALL be handled by the system

#### Scenario: Complete endpoint activates account

- **GIVEN** a user has entered their password on the setup page
- **WHEN** POST /api/auth/password-setup/complete is called with valid token and password
- **THEN** the system SHALL complete the password setup process
- **AND** return 200 OK with {message: "Success", registrationNumber}
- **AND** return appropriate error responses (400, 404, 410) for validation failures
- **AND** the endpoint SHALL be handled by the system

#### Scenario: Request endpoint issues new token

- **GIVEN** a user needs a new token because previous expired
- **WHEN** POST /api/auth/password-setup/request is called with {registrationNumber}
- **THEN** the system SHALL process the token reissuance request
- **AND** return 200 OK with generic success message (regardless of user existence)
- **AND** return 429 Too Many Requests with Retry-After header if rate limited
- **AND** return 400 Bad Request if account already active
- **AND** the endpoint SHALL be handled by the system

#### Scenario: CORS headers included in responses

- **GIVEN** a request from the configured frontend origin
- **WHEN** any password setup endpoint is called
- **THEN** the response SHALL include appropriate CORS headers
- **AND** the frontend SHALL be able to access the response

### Requirement: UserCreatedEvent Contains Type-Safe User Identifier

The system SHALL publish `UserCreatedEvent` with a type-safe user identifier field.

#### Scenario: UserCreatedEvent published with type-safe identifier

- **WHEN** a new User is created
- **THEN** a `UserCreatedEvent` is published with a user-specific identifier type
- **AND** event consumers receive type-safe user identifier that can be used directly without type conversion

#### Scenario: UserCreatedEvent identifier access

- **WHEN** accessing the user identifier from `UserCreatedEvent`
- **THEN** the identifier is of user-specific type
- **AND** no manual conversion from generic types is required by event consumers

### Requirement: Type-Safe User Identification

The system SHALL enforce type-safe user identification to prevent confusion between user identifiers and other entity identifiers.

#### Scenario: User services require user-specific identifier

- **WHEN** calling user permission, authentication, or query services
- **THEN** services require a user-specific identifier
- **AND** the system prevents accidental use of member or event identifiers where user identifier is required

#### Scenario: User domain uses user identifier type

- **WHEN** accessing a User entity's identifier
- **THEN** the identifier is of user-specific type
- **AND** implicit conversion to other identifier types is not allowed

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

### Requirement: User creation with optional email

The system SHALL support creating a user account with optional email address to enable coordination during member
registration.

#### Scenario: Create user with email during member registration

- **WHEN** a new member is registered with an email address provided
- **THEN** the system SHALL create a user account associated with the email
- **AND** the system SHALL store the email information for subsequent activation

#### Scenario: Create user without email

- **WHEN** a user account is created independently of member registration
- **THEN** the system SHALL create the user account without email information
- **AND** the user account SHALL be functional without email

### Requirement: User creation with flexible parameters

The system SHALL support creating user accounts with optional personally identifiable information to accommodate
different creation contexts.

#### Scenario: Create user account with PII

- **WHEN** a user account is created with optional personal information
- **THEN** the system SHALL accept and store the provided information
- **AND** the system SHALL create the user account in pending activation state

#### Scenario: Create user account without PII

- **WHEN** a user account is created without optional personal information
- **THEN** the system SHALL create the user account with default parameters
- **AND** the user account SHALL be created successfully

### Requirement: User Permissions Aggregate

The system SHALL manage user permissions as a separate aggregate from user identity, containing only direct authorities
assigned to the user.

**Rationale**: Separating permissions from identity allows authorization to scale independently and enables contextual
authorization checks.

#### Scenario: UserPermissions created for user

- **WHEN** a UserPermissions aggregate is created for a user
- **THEN** the aggregate contains the user's UserId
- **AND** the aggregate contains set of direct authorities
- **AND** the aggregate is persisted separately from User entity

#### Scenario: UserPermissions defaults to empty authorities

- **WHEN** a UserPermissions aggregate is created without specifying authorities
- **THEN** the aggregate is created with empty authorities set
- **AND** no default authorities are assigned

### Requirement: Grant Direct Authority

The system SHALL allow granting direct authorities to a user, subject to validation and business rules.

#### Scenario: Grant authority to user successfully

- **WHEN** user with MEMBERS:PERMISSIONS authority grants authority to target user
- **AND** the authority is valid
- **AND** the operation does not violate admin lockout prevention
- **THEN** the authority is added to user's direct authorities
- **AND** the UserPermissions aggregate is updated

#### Scenario: Grant duplicate authority idempotent

- **WHEN** user grants authority that user already has
- **THEN** operation succeeds (idempotent)
- **AND** no duplicate authority is added

#### Scenario: Grant global authority enforces scope validation

- **WHEN** user attempts to grant authority to user via group mechanism
- **AND** the authority is a global authority (e.g., MEMBERS:PERMISSIONS, SYSTEM:ADMIN)
- **THEN** validation fails
- **AND** error message indicates global authorities cannot be granted via groups
- **AND** global authorities must be granted directly

### Requirement: Revoke Direct Authority

The system SHALL allow revoking direct authorities from a user, subject to admin lockout prevention.

#### Scenario: Revoke authority successfully

- **WHEN** user with MEMBERS:PERMISSIONS authority revokes authority from target user
- **AND** the operation does not violate admin lockout prevention
- **THEN** the authority is removed from user's direct authorities
- **AND** the UserPermissions aggregate is updated

#### Scenario: Revoke non-existent authority idempotent

- **WHEN** user revokes authority that user does not have
- **THEN** operation succeeds (idempotent)
- **AND** no error is raised

### Requirement: Authority Scope Classification

The system SHALL classify authorities as either global (not group-grantable) or context-specific (group-grantable).

#### Scenario: Global authorities cannot be granted via groups

- **GIVEN** an authority is classified as global (e.g., MEMBERS:PERMISSIONS, SYSTEM:ADMIN)
- **WHEN** system validates group authorization grant
- **AND** the group attempts to grant this global authority
- **THEN** validation fails
- **AND** error indicates global authorities must be granted directly

#### Scenario: Context-specific authorities can be granted via groups

- **GIVEN** an authority is classified as context-specific (e.g., TRAINING:VIEW, TRAINING:MANAGE)
- **WHEN** system validates group authorization grant
- **AND** the group attempts to grant this authority
- **THEN** validation succeeds
- **AND** authority can be granted via group mechanism

### Requirement: Authorization Context

The system SHALL support authorization checks based on context consisting of actor, resource owner, and required
authority.

**Rationale**: Contextual authorization enables future group-based delegation where permissions depend on whose data is
being accessed.

#### Scenario: AuthorizationContext captures actor, resourceOwner, and requiredAuthority

- **WHEN** an authorization check is performed
- **THEN** context includes actor (who is making the request)
- **AND** context includes resourceOwner (whose data/resource is being accessed)
- **AND** context includes requiredAuthority (what authority is needed)

#### Scenario: Self-access context has actor equal to resourceOwner

- **WHEN** user accesses their own data
- **THEN** AuthorizationContext has actor equal to resourceOwner
- **AND** authorization check proceeds normally (no special self-access rule)

### Requirement: Authorization Query

The system SHALL query both direct authorities and (future) group-based authorities when performing authorization
checks.

#### Scenario: Check authorization with direct authority

- **GIVEN** user has direct authority MEMBERS:READ
- **WHEN** authorization check is performed for context requiring MEMBERS:READ
- **THEN** check returns true (authorized)

#### Scenario: Check authorization without direct authority

- **GIVEN** user does not have direct authority MEMBERS:READ
- **AND** user is not member of any group that grants MEMBERS:READ
- **WHEN** authorization check is performed for context requiring MEMBERS:READ
- **THEN** check returns false (not authorized)

#### Scenario: Authorization check does not grant automatic self-access

- **GIVEN** user does not have MEMBERSHIP:EDIT_PERSONAL authority
- **WHEN** user attempts to edit their own firstName
- **THEN** authorization check returns false
- **AND** user cannot edit own data without required authority

#### Scenario: Authorization check for self-access with authority

- **GIVEN** user has MEMBERSHIP:VIEW authority
- **WHEN** user views their own profile
- **THEN** authorization check returns true
- **AND** user can view own profile

### Requirement: Admin Lockout Prevention

The system SHALL prevent removal of MEMBERS:PERMISSIONS authority from the last user who has it.

#### Scenario: Prevent removing MEMBERS:PERMISSIONS from last admin

- **WHEN** user attempts to revoke MEMBERS:PERMISSIONS authority
- **AND** target user currently has MEMBERS:PERMISSIONS
- **AND** only one active user has MEMBERS:PERMISSIONS in the system
- **THEN** operation fails
- **AND** error indicates last permission manager cannot be removed
- **AND** HTTP 409 Conflict is returned

#### Scenario: Allow removing MEMBERS:PERMISSIONS when multiple admins exist

- **WHEN** user attempts to revoke MEMBERS:PERMISSIONS authority
- **AND** target user currently has MEMBERS:PERMISSIONS
- **AND** two or more active users have MEMBERS:PERMISSIONS
- **THEN** operation succeeds
- **AND** authority is revoked

#### Scenario: Count active users with specific authority

- **WHEN** system needs to validate admin lockout prevention
- **THEN** repository provides method to count active users with specific authority
- **AND** count includes only users with account status ACTIVE
- **AND** count considers only direct authorities (not group-based)

### Requirement: Get User Permissions API

The system SHALL provide REST API endpoint for retrieving a user's direct authorities with proper authorization.

#### Scenario: Get user permissions successfully

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits GET request to /api/users/{id}/permissions
- **AND** target user exists
- **THEN** HTTP 200 OK is returned
- **AND** response includes user ID
- **AND** response includes list of direct authorities
- **AND** response does NOT include group-based authorities (implementation detail)
- **AND** response includes HATEOAS links

#### Scenario: Get permissions returns only direct authorities

- **GIVEN** user has direct authorities {MEMBERS:READ}
- **AND** user is member of groups that grant additional authorities
- **WHEN** GET /api/users/{id}/permissions is called
- **THEN** response includes only direct authorities {MEMBERS:READ}
- **AND** group-based authorities are NOT included in response
- **AND** effective authorization (direct + group) is evaluated at access time

#### Scenario: Unauthorized user cannot get permissions

- **WHEN** authenticated user without MEMBERS:PERMISSIONS authority attempts to get permissions
- **THEN** HTTP 403 Forbidden is returned
- **AND** error message indicates missing authority

#### Scenario: Get permissions for nonexistent user

- **WHEN** user with MEMBERS:PERMISSIONS authority requests permissions for nonexistent user ID
- **THEN** HTTP 404 Not Found is returned
- **AND** error message indicates user not found

### Requirement: Update User Permissions API

The system SHALL provide REST API endpoint for updating a user's direct authorities with proper authorization and
validation.

#### Scenario: Update user permissions successfully

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits PUT request to /api/users/{id}/permissions
- **AND** request body contains valid authority list
- **AND** operation does not violate admin lockout rule
- **THEN** system replaces user's direct authorities with provided list
- **AND** HTTP 200 OK is returned
- **AND** response includes user ID and updated authorities
- **AND** response includes HATEOAS links

#### Scenario: Update permissions with empty list rejected

- **WHEN** authenticated user submits request with empty authorities list []
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates at least one authority required
- **AND** existing permissions remain unchanged

#### Scenario: Update permissions with invalid authority

- **WHEN** request contains invalid authority string
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates which authority is invalid
- **AND** error message lists valid authorities

#### Scenario: Update permissions violates admin lockout

- **WHEN** request would remove MEMBERS:PERMISSIONS from last admin user
- **THEN** HTTP 409 Conflict is returned
- **AND** error message indicates operation would remove last permission manager
- **AND** existing permissions remain unchanged

### Requirement: Extensibility for Group-Based Authorization

The system SHALL be designed to support future group-based delegated authorization without breaking existing direct
authority functionality.

**Rationale**: Member groups feature will allow group admins to receive group authorities when accessing group members'
data.

#### Scenario: Authorization check supports future group integration

- **GIVEN** authorization check is performed
- **AND** groups feature does not exist yet
- **WHEN** check is executed
- **THEN** check evaluates direct authorities
- **AND** check design allows extension for group-based authorities
- **AND** no breaking changes to existing authorization logic

#### Scenario: Group-based authorization is not implemented yet

- **GIVEN** groups feature is not yet developed
- **WHEN** authorization check is performed
- **THEN** only direct authorities are evaluated
- **AND** group membership is not checked
- **AND** system behavior is well-defined for current state

### Requirement: Valid Authorities

The system SHALL restrict permission updates to a predefined set of valid authorities.

#### Scenario: Valid authorities list

- **GIVEN** the system defines valid authorities:
    - Global authorities: MEMBERS:PERMISSIONS, SYSTEM:ADMIN
    - Context-specific: TRAINING:*, TRAINING:VIEW, TRAINING:MANAGE
    - Context-specific: COMPETITIONS:*, COMPETITIONS:VIEW, COMPETITIONS:MANAGE
    - Context-specific: MEMBERSHIP:*, MEMBERSHIP:VIEW, MEMBERSHIP:MANAGE
- **WHEN** user requests to set permissions with any valid authority
- **THEN** validation passes

#### Scenario: Invalid authority rejected with helpful error

- **WHEN** request includes invalid authority
- **THEN** error response includes complete list of valid authorities
- **AND** error message clearly identifies which authority was invalid
