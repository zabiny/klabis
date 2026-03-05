## MODIFIED Requirements

### Requirement: UserInfo Endpoint

The system SHALL provide a UserInfo endpoint at `/oauth2/userinfo` that returns claims about the authenticated user according to OIDC specification. The endpoint MUST accept valid access tokens and return user information as JSON, with claims filtered based on authorized scopes.

The UserInfo endpoint MUST:

- Require authentication via valid access token (Bearer token)
- Return claims for the authenticated user (identified by access token `sub` claim)
- Filter returned claims based on authorized scopes in the access token
- Support both JSON and JWT response formats
- Return HTTP 401 Unauthorized for invalid or expired access tokens
- Return HTTP 403 Forbidden if access token lacks required scopes (at minimum `openid`)
- Omit claims when underlying data is unavailable (null-safe behavior)

**Scope-based claim mapping:**

| Scope | Claims Returned | Requirements |
|-------|-----------------|--------------|
| `openid` | `sub` | Always returned (subject identifier) |
| `profile` | `given_name`, `family_name`, `registrationNumber`, `updated_at` | Returned only if Member entity exists |
| `email` | `email`, `email_verified` | Returned only if Member entity exists AND email is not null |

**Standard UserInfo claims:**

- `sub`: Subject identifier (required, always present - matches registrationNumber for members, username for admin users)
- `given_name`: User's first name (OIDC standard, requires `profile` scope)
- `family_name`: User's last name (OIDC standard, requires `profile` scope)
- `registrationNumber`: User's registration number (custom claim, requires `profile` scope, only for members)
- `updated_at`: Unix timestamp of when user profile was last modified (OIDC standard, requires `profile` scope)
- `email`: User's email address (OIDC standard, requires `email` scope)
- `email_verified`: Boolean indicating if email is verified (OIDC standard, requires `email` scope, always false until verification implemented)

**Edge case handling:**

- Admin users (no Member entity): Return only `sub` claim regardless of requested scopes
- Member without email: Omit `email` and `email_verified` claims even if `email` scope is present
- Missing Member entity: Omit all `profile` and `email` claims

#### Scenario: UserInfo endpoint returns only subject with openid scope

- **GIVEN** user has valid access token
- **AND** access token includes only `openid` scope
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `sub` claim matching user's identifier
- **AND** response does not include profile claims (given_name, family_name, registrationNumber)
- **AND** response does not include email claims (email, email_verified)

#### Scenario: UserInfo endpoint returns profile claims with profile scope

- **GIVEN** member user has valid access token
- **AND** access token includes `openid` and `profile` scopes
- **AND** user has linked Member entity with firstName and lastName
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `sub` claim matching user's registrationNumber
- **AND** response includes `given_name` claim with member's first name
- **AND** response includes `family_name` claim with member's last name
- **AND** response includes `registrationNumber` claim with member's registration number
- **AND** response includes `updated_at` claim with Unix timestamp of profile last modification
- **AND** response does not include sensitive information (password hash, authorities)

#### Scenario: UserInfo endpoint returns email claims with email scope

- **GIVEN** member user has valid access token
- **AND** access token includes `openid` and `email` scopes
- **AND** user has linked Member entity with email address
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `sub` claim matching user's registrationNumber
- **AND** response includes `email` claim with member's email address
- **AND** response includes `email_verified` claim with value false

#### Scenario: UserInfo endpoint omits email claims when member has no email

- **GIVEN** member user has valid access token
- **AND** access token includes `openid`, `profile`, and `email` scopes
- **AND** user has linked Member entity but email is null
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes `sub` claim
- **AND** response includes `given_name` and `family_name` claims (profile scope satisfied)
- **AND** response does not include `email` claim
- **AND** response does not include `email_verified` claim

#### Scenario: UserInfo endpoint returns only subject for admin user

- **GIVEN** admin user has valid access token
- **AND** access token includes `openid`, `profile`, and `email` scopes
- **AND** user has no linked Member entity (admin account)
- **WHEN** client sends GET request to `/oauth2/userinfo` with Authorization: Bearer <access_token>
- **THEN** system returns HTTP 200 OK with Content-Type: application/json
- **AND** response includes only `sub` claim with admin username
- **AND** response does not include profile claims (no Member entity)
- **AND** response does not include email claims (no Member entity)

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

#### Scenario: UserInfo endpoint rejects request without openid scope

- **GIVEN** user has valid access token without `openid` scope
- **WHEN** client sends GET request to `/oauth2/userinfo` with access token
- **THEN** system returns HTTP 403 Forbidden
- **AND** response indicates insufficient scope
