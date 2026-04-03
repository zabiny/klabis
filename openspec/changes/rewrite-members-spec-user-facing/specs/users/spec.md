## MODIFIED Requirements

### Requirement: User Authentication

The system SHALL authenticate users via OAuth2 using registration number as username and a cryptographically hashed password. Authorities are loaded from the UserPermissions aggregate.

#### Scenario: User logs in with valid credentials

- **WHEN** user enters their registration number and password on the login page and submits
- **THEN** the system issues an access token and a refresh token
- **AND** the user is redirected to the application

#### Scenario: User logs in with invalid credentials

- **WHEN** user enters an incorrect registration number or password
- **THEN** the login page shows an error that the credentials are invalid

#### Scenario: Expired access token is rejected

- **WHEN** the user's access token has expired
- **THEN** the system automatically attempts to refresh using the refresh token, or prompts the user to log in again

### Requirement: User Authorization

The system SHALL authorize operations based on user authorities from the UserPermissions aggregate.

#### Scenario: User with required authority can access a protected feature

- **WHEN** authenticated user with MEMBERS:MANAGE authority navigates to a protected members management feature
- **THEN** the feature is accessible

#### Scenario: User without required authority is denied

- **WHEN** authenticated user without MEMBERS:MANAGE authority attempts to access members management
- **THEN** the system shows a permission denied error

#### Scenario: Unauthenticated user is redirected to login

- **WHEN** unauthenticated user tries to access a protected page
- **THEN** the system redirects them to the login page

### Requirement: Authentication Principal

The system SHALL use the user's UUID as the authentication principal name for resolving member associations.

#### Scenario: Authenticated user has their UUID as the principal

- **WHEN** user successfully authenticates
- **THEN** the system identifies the user by their UUID (not by registration number)
- **AND** this UUID is used to resolve the associated member record

### Requirement: Valid Authorities

The system SHALL restrict permission updates to a predefined set of valid authorities.

#### Scenario: Valid authorities can be assigned

- **WHEN** admin with MEMBERS:PERMISSIONS authority assigns any combination of valid authorities (MEMBERS:MANAGE, MEMBERS:READ, MEMBERS:PERMISSIONS, EVENTS:MANAGE, EVENTS:READ, CALENDAR:MANAGE) to a user
- **THEN** the assignment succeeds

#### Scenario: Invalid authority shows error

- **WHEN** admin attempts to assign an authority not in the valid set
- **THEN** the system shows an error listing the valid authorities

### Requirement: User Aggregate

The system SHALL manage user accounts as a separate aggregate from members. Users cannot be deleted, only disabled. The User aggregate stores only identity-related data (credentials, account status) — not authorities.

#### Scenario: User account is created during member registration

- **WHEN** a new member is registered
- **THEN** a user account is automatically created with a generated unique identifier
- **AND** the account is created in pending activation status

#### Scenario: User account can be suspended

- **WHEN** admin suspends a user account
- **THEN** subsequent login attempts with that account fail
- **AND** the associated member profile (if any) remains in the database

### Requirement: Token Lifecycle

The system SHALL issue access tokens (15-minute TTL) and refresh tokens (30-day TTL).

#### Scenario: User continues using the app after access token expiry

- **WHEN** the access token expires during an active session
- **THEN** the system uses the refresh token to obtain a new access token without requiring the user to log in again

#### Scenario: Refresh token is invalidated on logout

- **WHEN** user logs out
- **THEN** the refresh token is revoked
- **AND** subsequent attempts to refresh fail

### Requirement: OAuth2 Client Registration

The system SHALL register OAuth2 clients for frontend applications.

#### Scenario: Default web client is available after system initialization

- **WHEN** the system is initialized
- **THEN** an OAuth2 client "klabis-web" is registered and ready for the frontend application to use

### Requirement: OpenID Connect ID Token Authentication

The system SHALL support OpenID Connect ID token generation when clients request the `openid` scope.

#### Scenario: Login with openid scope returns ID token

- **WHEN** the frontend authenticates using the `openid` scope
- **THEN** the system returns an ID token along with the access and refresh tokens
- **AND** the ID token identifies the user by their registration number

#### Scenario: Login without openid scope returns only access tokens

- **WHEN** client authenticates without the `openid` scope
- **THEN** only access and refresh tokens are returned

### Requirement: OpenID Connect UserInfo Access

The system SHALL provide user profile information via the UserInfo endpoint for clients with the `openid` scope.

#### Scenario: Client with openid scope can access user profile

- **WHEN** client with a valid access token containing the `openid` scope requests user profile info
- **THEN** the system returns the user's profile claims (sub, registration number, first name, last name if available)

#### Scenario: Client without openid scope is denied UserInfo access

- **WHEN** client with a valid access token without the `openid` scope requests user profile info
- **THEN** the system shows a permission denied error

### Requirement: OpenID Connect Logout

The system SHALL support OIDC RP-initiated logout at `/oauth2/logout`.

#### Scenario: User logs out via OIDC endpoint

- **WHEN** client initiates logout with a registered redirect URI
- **THEN** the user's session is terminated
- **AND** the user is redirected to the registered post-logout URI

#### Scenario: Logout with unregistered redirect URI is rejected

- **WHEN** client provides an unregistered redirect URI during logout
- **THEN** the system rejects the request and keeps the session active

### Requirement: Bootstrap Admin User

The system SHALL provision an admin user with full permissions upon initialization.

#### Scenario: Admin user is available after system initialization

- **WHEN** the system is initialized for the first time
- **THEN** an admin user with registration number "admin" exists and can log in
- **AND** the admin can register the first member

### Requirement: Audit Trail with Authentication Context

The system SHALL record the authenticated user's registration number for all data modifications.

#### Scenario: Data modification records who made the change

- **WHEN** authenticated user creates or updates a record
- **THEN** the system stores the user's registration number as the creator or modifier

#### Scenario: System operations use a fallback auditor

- **WHEN** an automated operation (e.g., database migration, scheduled job) modifies data
- **THEN** the system records "system" as the auditor

### Requirement: Security Error Handling

The system SHALL show user-friendly error messages for authentication and authorization failures.

#### Scenario: Unauthenticated access shows login prompt

- **WHEN** unauthenticated user accesses a protected resource
- **THEN** the system redirects them to the login page

#### Scenario: Unauthorized access shows permission denied

- **WHEN** authenticated user without the required authority accesses a feature
- **THEN** the system shows a permission denied message explaining what authority is missing

### Requirement: Password Complexity Validation

The system SHALL enforce password complexity requirements: minimum 12 characters, maximum 128 characters, must contain at least one uppercase letter, one lowercase letter, one digit, one special character, and must not contain the user's registration number, first name, or last name.

#### Scenario: Valid password is accepted

- **WHEN** user enters a password meeting all complexity requirements
- **THEN** the form accepts the password

#### Scenario: Password too short shows error

- **WHEN** user enters a password with fewer than 12 characters
- **THEN** the form shows an error about the minimum length requirement

#### Scenario: Password too long shows error

- **WHEN** user enters a password exceeding 128 characters
- **THEN** the form shows an error about the maximum length requirement

#### Scenario: Password missing uppercase letter shows error

- **WHEN** user enters a password without any uppercase letter
- **THEN** the form shows an error about the uppercase letter requirement

#### Scenario: Password missing lowercase letter shows error

- **WHEN** user enters a password without any lowercase letter
- **THEN** the form shows an error about the lowercase letter requirement

#### Scenario: Password missing digit shows error

- **WHEN** user enters a password without any digit
- **THEN** the form shows an error about the digit requirement

#### Scenario: Password missing special character shows error

- **WHEN** user enters a password without any special character
- **THEN** the form shows an error about the special character requirement

#### Scenario: Password containing personal information shows error

- **WHEN** user enters a password that contains their registration number, first name, or last name
- **THEN** the form shows an error that the password cannot contain personal information

#### Scenario: Multiple validation failures shown together

- **WHEN** user enters a password that fails multiple requirements
- **THEN** the form shows all validation errors at once

### Requirement: Password Setup Token Generation

The system SHALL generate a secure, time-limited password setup token (valid for 4 hours) when a new user account is created with PENDING_ACTIVATION status.

#### Scenario: Password setup token generated on member registration

- **WHEN** a new member is registered
- **THEN** a secure password setup token is generated
- **AND** a password setup email is sent to the member's primary email address

#### Scenario: Email not sent when member has no email

- **WHEN** a user account is created without an email address
- **THEN** no password setup email is sent
- **AND** the user account is created successfully

### Requirement: Password Setup Token Validation

The system SHALL validate password setup tokens and show appropriate messages for expired or invalid tokens.

#### Scenario: Member opens a valid activation link

- **WHEN** member clicks the activation link from the email within 4 hours
- **THEN** the password setup page loads and allows the user to set their password

#### Scenario: Member opens an expired activation link

- **WHEN** member clicks the activation link after 4 hours
- **THEN** the page shows an error that the activation link has expired
- **AND** provides an option to request a new link

#### Scenario: Member opens an already-used activation link

- **WHEN** member clicks an activation link that has already been used
- **THEN** the page shows an error that the activation link is no longer valid

### Requirement: Password Setup Completion

The system SHALL allow users to set their password using a valid token and activate their account.

#### Scenario: Member sets their password successfully

- **WHEN** member enters a valid password and matching confirmation on the password setup page
- **AND** submits the form
- **THEN** the account is activated
- **AND** the member can log in immediately

#### Scenario: Password confirmation mismatch shows error

- **WHEN** member enters a password and confirmation that do not match
- **THEN** the form shows an error that the passwords do not match

#### Scenario: Password not meeting complexity requirements shows errors

- **WHEN** member enters a password that does not meet complexity requirements
- **THEN** the form shows specific errors for each failed requirement
- **AND** the token remains valid

### Requirement: Password Setup Token Reissuance

The system SHALL allow users with PENDING_ACTIVATION status to request a new token if the previous one expired, with rate limiting (3 requests per hour, minimum 10 minutes between requests).

#### Scenario: Member requests a new activation token

- **WHEN** member submits their registration number to request a new activation link
- **AND** they have not exceeded the rate limit
- **THEN** a new password setup email is sent to their email address

#### Scenario: Rate limit exceeded shows retry information

- **WHEN** member has already requested 3 tokens in the past hour
- **THEN** the system shows an error with information about when they can try again

#### Scenario: Account already active shows appropriate message

- **WHEN** member with an already-active account requests a new activation token
- **THEN** the system shows that the account is already active and directs them to log in

### Requirement: User Permissions Aggregate

The system SHALL manage user permissions as a separate aggregate from user identity, containing only direct authorities assigned to the user.

#### Scenario: New user starts with no authorities

- **WHEN** a new user account is created
- **THEN** the user has no permissions by default
- **AND** permissions must be explicitly granted by an admin

### Requirement: Grant Direct Authority

The system SHALL allow users with MEMBERS:PERMISSIONS authority to grant authorities to other users.

#### Scenario: Admin grants an authority to a user

- **WHEN** user with MEMBERS:PERMISSIONS authority adds an authority to another user's permissions
- **THEN** the authority is assigned and takes effect immediately

#### Scenario: Granting an authority the user already has is harmless

- **WHEN** admin grants an authority a user already has
- **THEN** the operation succeeds without error and no duplicate is added

### Requirement: Revoke Direct Authority

The system SHALL allow users with MEMBERS:PERMISSIONS authority to revoke authorities from users.

#### Scenario: Admin revokes an authority from a user

- **WHEN** user with MEMBERS:PERMISSIONS authority removes an authority from another user's permissions
- **THEN** the authority is removed and the change takes effect immediately

#### Scenario: Revoking an authority the user does not have is harmless

- **WHEN** admin tries to revoke an authority a user does not have
- **THEN** the operation succeeds without error

### Requirement: Admin Lockout Prevention

The system SHALL prevent removal of the MEMBERS:PERMISSIONS authority from the last user who holds it.

#### Scenario: Last admin cannot remove their own MEMBERS:PERMISSIONS

- **WHEN** user attempts to revoke MEMBERS:PERMISSIONS from the only remaining user who has it
- **THEN** the system shows an error that the last permission manager cannot be removed

#### Scenario: MEMBERS:PERMISSIONS can be removed when multiple admins exist

- **WHEN** user attempts to revoke MEMBERS:PERMISSIONS from a user
- **AND** at least one other user also has MEMBERS:PERMISSIONS
- **THEN** the authority is revoked successfully

### Requirement: Get User Permissions API

The system SHALL allow users with MEMBERS:PERMISSIONS authority to view another user's direct authorities.

#### Scenario: Admin views user's permissions

- **WHEN** user with MEMBERS:PERMISSIONS authority opens the permissions page for a user
- **THEN** the current list of directly assigned authorities is shown

#### Scenario: Non-admin cannot view user permissions

- **WHEN** user without MEMBERS:PERMISSIONS authority attempts to view permissions for any user
- **THEN** the system shows a permission denied error

### Requirement: Update User Permissions API

The system SHALL allow users with MEMBERS:PERMISSIONS authority to update a user's direct authorities.

#### Scenario: Admin updates user permissions

- **WHEN** user with MEMBERS:PERMISSIONS authority changes the authority list and saves
- **THEN** the user's direct authorities are replaced with the new list

#### Scenario: Update that removes last admin shows error

- **WHEN** admin submits a permissions update that would remove MEMBERS:PERMISSIONS from the last admin
- **THEN** the system shows an error that the operation would remove the last permission manager
- **AND** no changes are saved
