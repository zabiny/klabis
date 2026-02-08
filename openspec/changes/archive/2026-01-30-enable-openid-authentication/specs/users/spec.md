# users Specification - Delta

## Purpose

This delta specification extends the existing users specification to add OpenID Connect (OIDC) support on top of OAuth2
authentication. The changes are backward compatible - existing OAuth2 flows continue to work unchanged. OIDC features
are opt-in via the `openid` scope.

**Note**: This change adds NEW capabilities (ID tokens, UserInfo endpoint) but does NOT modify existing OAuth2
authentication requirements. Existing scenarios in the users specification remain unchanged.

## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: OAuth2 Client Registration

**Updated from existing specification**: The default OAuth2 client registration SHALL include `openid` scope and OIDC
client metadata to enable OpenID Connect authentication out of the box.

**Previous behavior**: Default client registered with scopes: members.read, members.write

**New behavior**: Default client registered with scopes: openid, members.read, members.write

All other aspects of client registration (grant types, redirect URIs, authentication) remain unchanged.

#### Scenario: Bootstrap OAuth2 client includes openid scope

- **WHEN** the system is initialized
- **THEN** OAuth2 client "klabis-web" registered with client_id and client_secret
- **AND** client configured with grant types: authorization_code, refresh_token, client_credentials
- **AND** client configured with scopes: openid, members.read, members.write
- **AND** client configured with redirect URIs for authorization code flow
- **AND** client can perform both OAuth2 and OIDC authentication flows

**All other scenarios from the existing OAuth2 Client Registration requirement remain unchanged.**

---

## Impact Summary

### Added Functionality

- ID token generation when `openid` scope is requested
- UserInfo endpoint for user profile data
- RP-initiated logout endpoint
- OIDC provider configuration discovery endpoint

### Modified Functionality

- Default OAuth2 client now includes `openid` scope (backward compatible - existing clients continue to work)

### Unchanged Functionality

- OAuth2 authentication flows (without `openid` scope) work exactly as before
- Access token structure and claims remain unchanged
- Token lifetimes (15 min access, 30 day refresh) unchanged
- User credentials validation unchanged
- Authority/permission checks unchanged
- Token refresh flow unchanged

### Backward Compatibility

- All existing OAuth2 clients continue to work without modification
- Existing API endpoints unchanged
- Existing token validation unchanged
- ID tokens are opt-in via `openid` scope - clients not requesting `openid` see no difference
