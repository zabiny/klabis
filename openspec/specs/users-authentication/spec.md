# users-authentication Specification

## Purpose

This specification defines requirements for OpenID Connect Core 1.0 authentication layer on top of OAuth2. OpenID
Connect enables:

- ID tokens for user identity verification (authentication vs authorization)
- Standard discovery mechanism for clients to find provider configuration
- UserInfo endpoint for retrieving user profile information
- RP-initiated logout for single sign-out scenarios
- Federation with external identity providers (future requirement)

OpenID Connect builds on the existing OAuth2 authorization infrastructure, adding identity layer capabilities without
breaking existing OAuth2 flows. Clients can opt-in to OIDC features by requesting the `openid` scope.

## Requirements

### Requirement: OpenID Connect Discovery

The system SHALL provide an OIDC provider configuration endpoint at `/.well-known/openid-configuration` that returns
metadata about the OpenID Connect provider.

The discovery endpoint MUST be accessible via HTTP GET without authentication and MUST return JSON metadata containing:

- `issuer`: The issuer identifier for the authorization server
- `authorization_endpoint`: URL of the OAuth2 authorization endpoint
- `token_endpoint`: URL of the OAuth2 token endpoint
- `jwks_uri`: URL of the JSON Web Key Set endpoint
- `userinfo_endpoint`: URL of the UserInfo endpoint
- `end_session_endpoint`: URL of the logout endpoint
- `response_types_supported`: Array of supported OAuth2 response types (e.g., "code", "id_token")
- `subject_types_supported`: Array of supported subject types (e.g., "public")
- `id_token_signing_alg_values_supported`: Array of supported JWS signing algorithms (e.g., "RS256")
- `scopes_supported`: Array of supported OAuth2/OIDC scopes

#### Scenario: Client discovers OIDC provider configuration

- **WHEN** client sends GET request to `/.well-known/openid-configuration`
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes all required OIDC metadata fields
- **AND** `issuer` matches the authorization server's issuer URL
- **AND** all endpoint URLs are absolute HTTPS URLs
- **AND** response is cacheable with appropriate cache headers

#### Scenario: Discovery endpoint is publicly accessible

- **WHEN** unauthenticated client sends GET request to `/.well-known/openid-configuration`
- **THEN** system returns HTTP 200 OK without requiring authentication
- **AND** no WWW-Authenticate header is present
- **AND** response includes provider configuration metadata

#### Scenario: Discovery endpoint returns CORS headers for frontend

- **WHEN** frontend from configured origin sends OPTIONS preflight request to `/.well-known/openid-configuration`
- **THEN** system returns CORS headers allowing access from configured frontend origins
- **AND** subsequent GET request from frontend succeeds

---

### Requirement: ID Token Generation

The system SHALL generate ID tokens when clients request authentication with the `openid` scope. ID tokens MUST be JSON
Web Tokens (JWT) signed with RS256 algorithm and MUST contain standard OIDC claims.

ID tokens MUST include the following claims:

- `iss`: Issuer identifier (authorization server URL)
- `sub`: Subject identifier (user's unique identifier, using registrationNumber)
- `aud`: Audience (client_id of the OAuth2 client)
- `exp`: Expiration time (numeric date)
- `iat`: Issued at time (numeric date)
- `auth_time`: Time when authentication occurred (numeric date)
- `nonce`: Nonce value from authorization request (if provided)

ID tokens MAY include additional claims:

- `registrationNumber`: User's registration number

ID tokens MUST have a limited lifetime (typically 5-15 minutes) and MUST be stored in the `oauth2_authorization` table
for record-keeping.

#### Scenario: Authorization code flow returns ID token

- **GIVEN** client requests authorization with `response_type=code` and `scope=openid`
- **AND** user successfully authenticates
- **WHEN** client exchanges authorization code for tokens
- **THEN** system returns access token, refresh token, and ID token
- **AND** ID token is signed JWT with RS256
- **AND** ID token contains standard OIDC claims (iss, sub, aud, exp, iat, auth_time)
- **AND** ID token `sub` claim equals user's registrationNumber
- **AND** ID token includes `registrationNumber` claim
- **AND** ID token does NOT include firstName or lastName (available via UserInfo endpoint)

#### Scenario: Implicit flow returns ID token directly

- **GIVEN** client requests authorization with `response_type=id_token` and `scope=openid`
- **AND** user successfully authenticates
- **WHEN** authorization response is returned to client via redirect
- **THEN** response includes ID token in URL fragment
- **AND** ID token contains standard OIDC claims
- **AND** ID token is validated by client using JWKS endpoint

#### Scenario: ID token expires and is rejected

- **GIVEN** client has expired ID token (exp < current time)
- **WHEN** client attempts to use expired ID token for authentication
- **THEN** ID token validation fails
- **AND** system returns authentication error indicating token expired
- **AND** client must re-authenticate to obtain new ID token

#### Scenario: ID token stored in database after generation

- **GIVEN** user authenticates with `openid` scope
- **WHEN** ID token is generated
- **THEN** ID token value is stored in `oauth2_authorization.oidc_id_token_value`
- **AND** ID token issued time is stored in `oauth2_authorization.oidc_id_token_issued_at`
- **AND** ID token expiration time is stored in `oauth2_authorization.oidc_id_token_expires_at`
- **AND** ID token metadata is stored in `oauth2_authorization.oidc_id_token_metadata`

#### Scenario: Authorization without openid scope does not return ID token

- **GIVEN** client requests authorization with `scope=profile` (no `openid`)
- **WHEN** client exchanges authorization code for tokens
- **THEN** system returns access token and refresh token only
- **AND** no ID token is generated or returned

---

### Requirement: UserInfo Endpoint

The system SHALL provide a UserInfo endpoint at `/oauth2/userinfo` that returns claims about the authenticated user. The
endpoint MUST accept valid access tokens and return user profile information as JSON.

The UserInfo endpoint MUST:

- Require authentication via valid access token (Bearer token)
- Return claims for the authenticated user (identified by access token `sub` claim)
- Support both JSON and JWT response formats
- Return HTTP 401 Unauthorized for invalid or expired access tokens
- Return HTTP 403 Forbidden if access token lacks required scopes

Standard UserInfo claims:

- `sub`: Subject identifier (required, matches ID token)
- `registrationNumber`: User's registration number
- `firstName`: User's first name
- `lastName`: User's last name

#### Scenario: UserInfo endpoint returns user profile

- **GIVEN** user has valid access token
- **AND** access token includes `openid` scope
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `sub` claim matching user's registrationNumber
- **AND** response includes `registrationNumber` claim
- **AND** response includes `firstName` claim
- **AND** response includes `lastName` claim
- **AND** response does not include sensitive information (password hash, authorities)

#### Scenario: UserInfo endpoint rejects expired access token

- **GIVEN** user has expired access token
- **WHEN** client sends GET request to `/oauth2/userinfo` with expired access token
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response indicates token is expired or invalid
- **AND** no user claims are returned

#### Scenario: UserInfo endpoint rejects request without access token

- **WHEN** client sends GET request to `/oauth2/userinfo` without Authorization header
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response includes WWW-Authenticate: Bearer header

#### Scenario: UserInfo endpoint respects token scope

- **GIVEN** user has valid access token without `openid` scope
- **WHEN** client sends GET request to `/oauth2/userinfo` with access token
- **THEN** system returns HTTP 403 Forbidden
- **AND** response indicates insufficient scope

---

### Requirement: RP-Initiated Logout

The system SHALL provide a logout endpoint at `/oauth2/logout` that enables relying parties to initiate user logout and
terminate the user's session.

The logout endpoint MUST:

- Accept POST requests with logout parameters
- Validate the post-logout redirect URI against registered client URIs
- Accept optional `state` parameter to maintain session state
- Accept optional `post_logout_redirect_uri` parameter for redirect after logout
- Terminate the user's session and invalidate tokens
- Redirect to post-logout redirect URI upon successful logout

Logout parameters:

- `post_logout_redirect_uri`: URI to redirect after logout (optional, must match client registration)
- `state`: Opaque value to maintain state between request and callback (optional, recommended)
- `id_token_hint`: ID token previously issued to the client (optional, recommended for verification)

#### Scenario: RP-initiated logout terminates session

- **GIVEN** user has active session with valid tokens
- **AND** client has registered post-logout redirect URI
- **WHEN** client sends POST request to `/oauth2/logout` with valid post_logout_redirect_uri
- **THEN** system terminates user's session
- **AND** system invalidates refresh tokens
- **AND** system redirects to post_logout_redirect_uri
- **AND** session cleanup is logged for audit

#### Scenario: Logout validates post-logout redirect URI

- **GIVEN** client has registered post-logout redirect URIs: `https://client.com/callback`
- **WHEN** client sends POST request to `/oauth2/logout` with `post_logout_redirect_uri=https://evil.com`
- **THEN** system rejects logout request
- **AND** system returns HTTP 400 Bad Request
- **AND** response indicates invalid redirect URI
- **AND** user session remains active

#### Scenario: Logout with state parameter returns state

- **GIVEN** client has registered post-logout redirect URI
- **WHEN** client sends POST request to `/oauth2/logout` with `post_logout_redirect_uri` and `state=abc123`
- **THEN** system terminates session and redirects
- **AND** redirect URL includes `state=abc123` parameter
- **AND** client can verify state matches original value

#### Scenario: Logout without redirect URI returns default

- **GIVEN** user has active session
- **WHEN** client sends POST request to `/oauth2/logout` without post_logout_redirect_uri
- **THEN** system terminates user's session
- **AND** system returns HTTP 200 OK with logout confirmation
- **AND** response indicates successful logout

#### Scenario: Logout with id_token_hint verifies user

- **GIVEN** client has valid ID token for user
- **WHEN** client sends POST request to `/oauth2/logout` with `id_token_hint=<id_token>`
- **THEN** system validates ID token signature and claims
- **AND** system identifies user from ID token `sub` claim
- **AND** system terminates user's session
- **AND** system redirects to post_logout_redirect_uri

---

### Requirement: OpenID Scope Support

The system SHALL support the `openid` scope as the indicator that the client intends to use OpenID Connect features.
When `openid` scope is requested, the system MUST enable OIDC-specific behavior (ID token generation, UserInfo access,
etc.).

The `openid` scope:

- MUST be included in all OIDC requests
- Triggers ID token generation in token responses
- Enables access to UserInfo endpoint
- Can be combined with other scopes (e.g., `openid profile email`)
- MUST be registered in OAuth2 client scope configuration

#### Scenario: Client requests openid scope

- **GIVEN** OAuth2 client is registered with `openid` in allowed scopes
- **WHEN** client requests authorization with `scope=openid`
- **THEN** authorization request is accepted
- **AND** subsequent token request returns ID token along with access token
- **AND** access token includes `openid` in scope claim

#### Scenario: OpenID scope combined with other scopes

- **GIVEN** client is registered with `openid`, `profile`, and `members.read` scopes
- **WHEN** client requests authorization with `scope=openid members.read`
- **THEN** authorization request is accepted
- **AND** token response includes ID token
- **AND** access token includes `openid`, `members.read` in scope claim
- **AND** user can access UserInfo endpoint and members API

#### Scenario: Client without openid scope cannot get ID token

- **GIVEN** client requests authorization with `scope=profile` (no `openid`)
- **WHEN** client exchanges authorization code for tokens
- **THEN** token response does NOT include ID token
- **AND** client cannot access UserInfo endpoint (insufficient scope error)

#### Scenario: Default OAuth2 client includes openid scope

- **GIVEN** system is initialized with default OAuth2 client
- **WHEN** default client is registered in database
- **THEN** client's allowed scopes include `openid`
- **AND** client can request OIDC authentication flows
- **AND** client can use discovery, UserInfo, and logout endpoints

---

### Requirement: OIDC Client Configuration

The system SHALL support OIDC-specific client metadata in OAuth2 client registrations. Client metadata MUST include
settings for OIDC compliance and interoperability.

Required OIDC client metadata:

- `scope`: MUST include `openid` for OIDC clients
- `response_types`: Supported OAuth2 response types (e.g., "code", "id_token", "code id_token")
- `grant_types`: Supported grant types (e.g., "authorization_code", "implicit")
- `redirect_uris`: Allowed redirect URIs for authorization flow
- `post_logout_redirect_uris`: Allowed redirect URIs for logout (optional)
- `token_endpoint_auth_method`: Client authentication method at token endpoint

Client configuration settings:

- `id_token_signed_response_alg`: JWS algorithm for ID token signing (default: RS256)
- `subject_type`: Subject identifier type (default: "public")
- `require_auth_time`: Whether `auth_time` claim is required in ID token (default: true)

#### Scenario: OIDC client registered with required metadata

- **GIVEN** system is being configured
- **WHEN** OAuth2 client is registered with OIDC metadata
- **THEN** client includes `openid` in allowed scopes
- **AND** client supports required response types (e.g., "code", "id_token")
- **AND** client specifies `token_endpoint_auth_method`
- **AND** client can participate in OIDC authentication flows

#### Scenario: Client configuration specifies ID token algorithm

- **GIVEN** OAuth2 client is registered with `id_token_signed_response_alg=RS256`
- **WHEN** ID token is generated for this client
- **THEN** ID token is signed using RS256 algorithm
- **AND** ID token header includes `"alg": "RS256"`
- **AND** client can verify ID token signature using JWKS endpoint

#### Scenario: Client with post-logout redirect URIs

- **GIVEN** OAuth2 client is registered with `post_logout_redirect_uris`
- **WHEN** client initiates logout with `post_logout_redirect_uri`
- **THEN** system validates redirect URI against registered URIs
- **AND** redirect is permitted only if URI matches registered value
- **AND** logout completes successfully

#### Scenario: Bootstrap client includes OIDC configuration

- **GIVEN** system is initialized with default OAuth2 client
- **WHEN** default client is created in database
- **THEN** client includes `openid` in scopes
- **AND** client supports `authorization_code` grant type
- **AND** client supports `code` and `id_token` response types
- **AND** client can perform OIDC authentication out of the box
