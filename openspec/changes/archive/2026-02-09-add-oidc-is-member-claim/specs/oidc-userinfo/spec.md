## MODIFIED Requirements

### Requirement: UserInfo Endpoint

The system SHALL provide a UserInfo endpoint at `/oauth2/userinfo` that returns claims about the authenticated user. The
endpoint MUST accept valid access tokens and return user profile information as JSON with scope-based claim filtering.

The UserInfo endpoint MUST:

- Require authentication via valid access token (Bearer token)
- Return claims for the authenticated user (identified by access token `sub` claim)
- Support both JSON and JWT response formats
- Return HTTP 401 Unauthorized for invalid or expired access tokens
- Return HTTP 403 Forbidden if access token lacks required scopes
- Filter claims based on authorized scopes (scope-based access control)
- Omit claims when underlying data is unavailable (null-safe)

Scope-to-Claims Mapping:

| Scope | Claims Returned | Condition |
|-------|----------------|-----------|
| `openid` | `sub` | Always (required) |
| `profile` | `user_name`, `is_member`, `given_name`, `family_name`, `updated_at` | Always when profile scope present |

OIDC Standard UserInfo claims:

- `sub`: Subject identifier (required, matches ID token) - always returned with `openid` scope
- `given_name`: User's given name - returned with `profile` scope when Member entity exists
- `family_name`: User's family name - returned with `profile` scope when Member entity exists
- `updated_at`: Profile last modification timestamp (Instant) - returned with `profile` scope when Member entity exists
- `email`: User's email address - returned with `email` scope when available
- `email_verified`: Boolean indicating email verification status - returned with `email` scope (currently always `false`)

Custom domain-specific claims:

- `user_name`: User's username (registration number or custom username) - returned with `profile` scope
- `is_member`: Boolean indicating whether user has an associated Member profile - returned with `profile` scope

#### Scenario: UserInfo endpoint returns only sub claim with openid scope

- **GIVEN** user has valid access token
- **AND** access token includes only `openid` scope
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes only `sub` claim matching user's username
- **AND** response does NOT include `user_name` claim
- **AND** response does NOT include `is_member` claim
- **AND** response does NOT include profile claims (given_name, family_name)
- **AND** response does NOT include email claims (email, email_verified)

#### Scenario: UserInfo endpoint returns profile claims for member with profile scope

- **GIVEN** user has valid access token
- **AND** access token includes `openid` and `profile` scopes
- **AND** user has associated Member entity
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `sub` claim matching user's username
- **AND** response includes `user_name` claim (user's username)
- **AND** response includes `is_member` claim with value `true`
- **AND** response includes `given_name` claim (user's first name)
- **AND** response includes `family_name` claim (user's last name)
- **AND** response includes `updated_at` claim (profile last modification timestamp)
- **AND** response does NOT include email claims without `email` scope

#### Scenario: UserInfo endpoint returns profile claims for admin with profile scope

- **GIVEN** admin user has valid access token
- **AND** access token includes `openid` and `profile` scopes
- **AND** admin user has NO associated Member entity
- **WHEN** admin sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `sub` claim (admin username)
- **AND** response includes `user_name` claim (admin username)
- **AND** response includes `is_member` claim with value `false`
- **AND** response does NOT include `given_name` claim (no Member entity exists)
- **AND** response does NOT include `family_name` claim (no Member entity exists)
- **AND** response does NOT include `updated_at` claim (no Member entity exists)
- **AND** response does NOT include email claims (no Member entity exists)

#### Scenario: UserInfo endpoint returns email claims with email scope

- **GIVEN** user has valid access token
- **AND** access token includes `openid` and `email` scopes
- **AND** user has associated Member entity with email address
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `sub` claim
- **AND** response includes `email` claim (user's email address)
- **AND** response includes `email_verified` claim (currently `false`)
- **AND** response does NOT include profile claims without `profile` scope

#### Scenario: UserInfo endpoint combines profile and email scopes for member

- **GIVEN** user has valid access token
- **AND** access token includes `openid`, `profile`, and `email` scopes
- **AND** user has associated Member entity with email address
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes all claims: `sub`, `user_name`, `is_member` (true), `given_name`, `family_name`, `updated_at`, `email`, `email_verified`

#### Scenario: UserInfo endpoint omits email claims when Member has no email

- **GIVEN** user has valid access token
- **AND** access token includes `openid`, `profile`, and `email` scopes
- **AND** user has associated Member entity WITHOUT email address (e.g., minor with guardian)
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes profile claims: `sub`, `user_name`, `is_member` (true), `given_name`, `family_name`, `updated_at`
- **AND** response does NOT include email claims (email is null)
- **AND** no error is returned (omitting claims is OIDC-compliant)

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
