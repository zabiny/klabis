# Users Authentication Specification

## Purpose

Covers the OpenID Connect authentication layer on top of OAuth2. Defines OIDC discovery, ID token generation, UserInfo endpoint with membership status detection, RP-initiated logout, account activation tokens, and membership detection.

## Requirements

### Requirement: OpenID Connect Discovery

The system SHALL provide an OIDC provider configuration endpoint that is publicly accessible without authentication, allowing clients to discover all provider endpoints and capabilities.

#### Scenario: Client discovers OIDC provider configuration

- **WHEN** client (or frontend application) requests the OIDC discovery endpoint
- **THEN** the system returns provider metadata including all required endpoint URLs
- **AND** the endpoint is publicly accessible without authentication

#### Scenario: Discovery endpoint is accessible to unauthenticated clients

- **WHEN** an unauthenticated client requests the discovery endpoint
- **THEN** the system returns the configuration without requiring a login

#### Scenario: Discovery endpoint allows CORS access for the frontend

- **WHEN** the frontend application sends a request to the discovery endpoint
- **THEN** the system includes CORS headers that allow the frontend origin to access the response

### Requirement: ID Token Generation

The system SHALL generate ID tokens (signed JWT, RS256) when clients request authentication with the `openid` scope.

#### Scenario: Authorization code flow with openid scope returns ID token

- **WHEN** user authenticates using authorization code flow with `openid` scope
- **THEN** the token response includes an access token, refresh token, and ID token
- **AND** the ID token identifies the user by their registration number
- **AND** the ID token does not include first name or last name (available via UserInfo endpoint)

#### Scenario: Authorization without openid scope does not return ID token

- **WHEN** client authenticates without requesting the `openid` scope
- **THEN** the token response includes only access token and refresh token

#### Scenario: Expired ID token is rejected

- **WHEN** user attempts to authenticate using an expired ID token
- **THEN** the system shows an authentication error indicating the token has expired
- **AND** the user must re-authenticate

### Requirement: UserInfo Endpoint

The system SHALL provide a UserInfo endpoint that returns profile claims based on the scopes included in the access token.

#### Scenario: UserInfo with only openid scope returns sub claim

- **WHEN** authenticated user requests the UserInfo endpoint with only `openid` scope
- **THEN** only the `sub` claim is returned

#### Scenario: UserInfo with profile scope returns profile claims for a member

- **WHEN** authenticated member requests the UserInfo endpoint with `openid` and `profile` scopes
- **THEN** the response includes: sub, user_name, is_member (true), given_name, family_name, updated_at

#### Scenario: UserInfo with profile scope for admin without member profile

- **WHEN** admin without a member profile requests the UserInfo endpoint with `profile` scope
- **THEN** the response includes: sub, user_name, is_member (false)
- **AND** given_name and family_name are not included

#### Scenario: UserInfo with email scope returns email claims

- **WHEN** authenticated user requests the UserInfo endpoint with `openid` and `email` scopes
- **AND** the user has a member profile with an email address
- **THEN** the response includes: sub, email, email_verified

#### Scenario: UserInfo omits email claims when member has no email

- **WHEN** authenticated user requests the UserInfo endpoint with `email` scope
- **AND** the user's member profile has no email (e.g., minor with guardian only)
- **THEN** email claims are not included in the response
- **AND** no error is returned

#### Scenario: UserInfo with expired access token returns error

- **WHEN** client sends an expired access token to the UserInfo endpoint
- **THEN** the system returns an authentication error

#### Scenario: UserInfo without access token returns error

- **WHEN** client sends a request to the UserInfo endpoint without an Authorization header
- **THEN** the system returns an authentication error

#### Scenario: UserInfo without openid scope returns permission denied

- **WHEN** client sends a valid access token without the `openid` scope to UserInfo
- **THEN** the system returns a permission denied error

### Requirement: RP-Initiated Logout

The system SHALL provide a logout endpoint that terminates the user's session and redirects to a registered post-logout URI.

#### Scenario: User logs out and is redirected

- **WHEN** user initiates logout with a registered post-logout redirect URI
- **THEN** the session is terminated and refresh tokens are invalidated
- **AND** the user is redirected to the post-logout URI

#### Scenario: Logout with unregistered redirect URI is rejected

- **WHEN** client initiates logout with a redirect URI not registered for the client
- **THEN** the logout is rejected and the user's session remains active

#### Scenario: Logout with state parameter returns state

- **WHEN** client initiates logout with a state parameter
- **THEN** the state value is included in the redirect URL after logout

#### Scenario: Logout without redirect URI shows confirmation

- **WHEN** user initiates logout without a post-logout redirect URI
- **THEN** the session is terminated and a logout confirmation is shown

### Requirement: OpenID Scope Support

The system SHALL support the `openid` scope to enable OIDC features (ID token, UserInfo access).

#### Scenario: Client requests openid scope

- **WHEN** client requests authorization with `openid` scope
- **THEN** the token response includes an ID token
- **AND** the access token includes `openid` in the scope claim

#### Scenario: Default OAuth2 client supports openid scope

- **WHEN** the system is initialized
- **THEN** the default client supports `openid`, `profile`, and `email` scopes out of the box

### Requirement: OIDC Client Configuration

The system SHALL support OIDC-specific client metadata for OAuth2 client registrations.

#### Scenario: Bootstrap client includes OIDC configuration

- **WHEN** the system is initialized with the default OAuth2 client
- **THEN** the client supports `authorization_code` grant type and `openid` scope
- **AND** the client can perform OIDC authentication flows

### Requirement: Membership Status Claim in UserInfo Response

The OIDC UserInfo endpoint SHALL include an `is_member` boolean claim when the `profile` scope is authorized.

#### Scenario: Member user receives is_member true claim

- **WHEN** user with an associated member profile requests UserInfo with `profile` scope
- **THEN** the response includes `"is_member": true`
- **AND** standard profile claims (given_name, family_name, updated_at) are included

#### Scenario: Admin user without member profile receives is_member false claim

- **WHEN** user without an associated member profile requests UserInfo with `profile` scope
- **THEN** the response includes `"is_member": false`
- **AND** member-specific profile claims are not included

#### Scenario: UserInfo without profile scope does not include is_member claim

- **WHEN** user requests UserInfo without `profile` scope
- **THEN** the `is_member` claim is not included in the response

### Requirement: Secure Activation Tokens

The system SHALL generate secure, time-limited activation tokens for email-based account activation. Tokens expire after 72 hours and are single-use.

#### Scenario: Member activates account via activation link

- **WHEN** member clicks the activation link from their welcome email within 72 hours
- **THEN** their account is activated
- **AND** the activation link can no longer be used

#### Scenario: Expired activation link shows error

- **WHEN** member clicks the activation link after 72 hours
- **THEN** the page shows an error that the activation link has expired

#### Scenario: Already-used activation link shows error

- **WHEN** member who has already activated their account clicks the activation link again
- **THEN** the page shows an error that the activation link is no longer valid

### Requirement: Account Activation Endpoint

The system SHALL provide an account activation endpoint accessible from the welcome email link.

#### Scenario: Successful activation via endpoint

- **WHEN** user follows a valid, unexpired activation link
- **THEN** the page shows a confirmation that the account was activated successfully

#### Scenario: Expired token via activation endpoint

- **WHEN** user follows an expired activation link
- **THEN** the page shows an error about the expired token

### Requirement: Membership Detection Based on Member Aggregate Existence

The system SHALL determine membership status by checking whether a Member aggregate exists for the authenticated user's registration number.

#### Scenario: User with a matching member record is identified as a member

- **WHEN** the authenticated user's registration number matches a member record
- **THEN** the system identifies the user as a member (`is_member: true`)

#### Scenario: User without a matching member record is identified as non-member

- **WHEN** the authenticated user's registration number does not match any member record
- **THEN** the system identifies the user as a non-member (`is_member: false`)

#### Scenario: Admin username not in registration number format is identified as non-member

- **WHEN** the authenticated username does not match the registration number format
- **THEN** the system identifies the user as a non-member without querying the member repository
