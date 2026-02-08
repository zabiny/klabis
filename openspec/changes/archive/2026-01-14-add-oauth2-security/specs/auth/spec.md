# Authentication and Authorization Specification

## ADDED Requirements

### Requirement: User Authentication

The system SHALL authenticate users via OAuth2 with Spring Authorization Server using registrationNumber as username and
BCrypt-hashed password.

#### Scenario: Successful authentication with valid credentials

- **WHEN** user submits valid registrationNumber and password to /oauth2/token
- **THEN** system returns JWT access token (15 min TTL) and opaque refresh token (30 day TTL)
- **AND** access token contains claims: registrationNumber, authorities, expiration

#### Scenario: Authentication fails with invalid credentials

- **WHEN** user submits invalid registrationNumber or password
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response includes error description "Invalid credentials"

#### Scenario: Token validation extracts user context

- **WHEN** API receives request with valid JWT access token
- **THEN** system extracts registrationNumber and authorities from token claims
- **AND** SecurityContext is populated with authenticated user

#### Scenario: Expired access token rejected

- **WHEN** API receives request with expired JWT access token
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response indicates token has expired

### Requirement: User Authorization

The system SHALL authorize API operations based on authorities derived from user roles and OAuth2 scopes.

#### Scenario: User with required authority accesses endpoint

- **WHEN** authenticated user with MEMBERS:CREATE authority calls POST /api/members
- **THEN** authorization check passes
- **AND** endpoint handler executes

#### Scenario: User without required authority denied access

- **WHEN** authenticated user without MEMBERS:CREATE authority calls POST /api/members
- **THEN** authorization check fails
- **AND** system returns HTTP 403 Forbidden with ProblemDetail

#### Scenario: Unauthenticated request denied

- **WHEN** unauthenticated request (no token) is made to secured endpoint
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response includes WWW-Authenticate header with Bearer challenge

### Requirement: User Aggregate

The system SHALL manage user accounts as a separate aggregate from members, linked via registrationNumber.

#### Scenario: User created with credentials and roles

- **WHEN** User.create is called with registrationNumber, password, and roles
- **THEN** User aggregate is created with BCrypt-hashed password
- **AND** User has unique ID (UUID)
- **AND** User accountStatus is set to specified value
- **AND** User can exist without linked Member

#### Scenario: User password changed

- **WHEN** User.changePassword is called with new password
- **THEN** password is BCrypt-hashed and stored
- **AND** User identity (registrationNumber) preserved

#### Scenario: User authorities derived from roles

- **WHEN** User.getAuthorities() is called
- **THEN** system maps roles to authorities (ROLE_ADMIN → MEMBERS:CREATE, MEMBERS:READ, etc.)
- **AND** authorities returned as GrantedAuthority collection

#### Scenario: User account suspended

- **WHEN** User.suspend() is called
- **THEN** accountStatus changed to SUSPENDED
- **AND** subsequent authentication attempts fail
- **AND** existing tokens remain valid until expiration

### Requirement: Token Lifecycle

The system SHALL issue JWT access tokens (15 min TTL) and opaque refresh tokens (30 day TTL).

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

### Requirement: Role-Based Access Control

The system SHALL define roles that map to sets of authorities (permissions).

#### Scenario: ROLE_ADMIN has all permissions

- **WHEN** user has ROLE_ADMIN
- **THEN** user granted authorities: MEMBERS:CREATE, MEMBERS:READ, MEMBERS:UPDATE, MEMBERS:DELETE
- **AND** future authorities automatically granted to ROLE_ADMIN

#### Scenario: ROLE_MEMBER has limited permissions

- **WHEN** user has ROLE_MEMBER
- **THEN** user granted authority: MEMBERS:READ (own data only, future enforcement)

#### Scenario: Multiple roles aggregate authorities

- **WHEN** user has both ROLE_ADMIN and ROLE_MEMBER
- **THEN** user granted union of all authorities from both roles

### Requirement: OAuth2 Client Registration

The system SHALL register OAuth2 clients for frontend applications to obtain tokens.

#### Scenario: Bootstrap OAuth2 client for web application

- **WHEN** database migration V002 executes
- **THEN** OAuth2 client "klabis-web" registered with client_id and client_secret
- **AND** client configured with grant types: authorization_code, refresh_token, client_credentials
- **AND** client configured with scopes: members.read, members.write
- **AND** client configured with redirect URIs for authorization code flow

#### Scenario: Client authenticates with credentials

- **WHEN** client submits client_id and client_secret to /oauth2/token
- **THEN** system validates client credentials
- **AND** client authorized to obtain tokens on behalf of users

### Requirement: Bootstrap Admin User

The system SHALL provision an admin user with full permissions upon database initialization.

#### Scenario: Admin user created in database migration

- **WHEN** database migration V002 executes
- **THEN** user created with registrationNumber "ZBM0001"
- **AND** user password is BCrypt hash of "admin123"
- **AND** user assigned ROLE_ADMIN
- **AND** user accountStatus is ACTIVE
- **AND** user has no linked Member (registrationNumber not in members table)

#### Scenario: Admin user authenticates and creates first member

- **WHEN** admin user authenticates with ZBM0001 / admin123
- **THEN** access token issued with MEMBERS:CREATE authority
- **AND** admin can call POST /api/members to create first member
- **AND** created member can have different registrationNumber than admin user

### Requirement: Audit Trail with Authentication Context

The system SHALL record authenticated user's registrationNumber as auditor for all data modifications.

#### Scenario: Authenticated user creates resource

- **WHEN** authenticated user with registrationNumber ZBM0001 creates member
- **THEN** created_by field populated with "ZBM0001"
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
