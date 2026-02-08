# Proposal: Enable OpenID Connect Authentication

## Why

Klabis currently implements OAuth2 authorization server with Spring Authorization Server, but does not have OpenID
Connect (OIDC) enabled. OIDC provides a standardized identity layer on top of OAuth2 that enables:

- ID tokens for user authentication (not just authorization)
- Standard discovery mechanism for clients
- UserInfo endpoint for user profile data
- Compatibility with standard OIDC clients and identity providers
- Federation with external identity providers (future requirement)

Spring Authorization Server has built-in OIDC support that requires only configuration changes - no custom endpoint
implementations needed. The database schema already includes OIDC token storage (`oidc_id_token_*` columns in
`oauth2_authorization` table). Enabling OIDC now provides a migration path from custom authentication to industry
standards, improving interoperability and reducing future maintenance burden.

## What Changes

Enable OpenID Connect Core 1.0 compliance through Spring Authorization Server configuration:

1. **Enable OIDC features in authorization server settings**
    - Add `oidcEnabled(true)` to `AuthorizationServerSettings` (automatically enables discovery endpoint)
    - Configure OIDC token customizer for ID token generation

2. **OIDC provider configuration endpoint** (built-in)
    - Spring Authorization Server automatically provides `/.well-known/openid-configuration`
    - Returns standard OIDC metadata (issuer, endpoints, supported scopes, response types, signing algorithms)

3. **Enable ID token generation**
    - Configure JWT customizer to include standard OIDC claims (`iss`, `sub`, `aud`, `exp`, `iat`, `auth_time`)
    - Add `openid` scope to supported scopes
    - Store ID tokens in existing database columns (`oauth2_authorization.oidc_id_token_*`)

4. **Configure UserInfo endpoint**
    - Enable Spring Authorization Server's built-in UserInfo endpoint at `/oauth2/userinfo`
    - Map user profile claims (registration number, authorities, preferences)

5. **Enable RP-initiated logout**
    - Configure logout endpoint at `/oauth2/logout`
    - Support post-logout redirect validation
    - Maintain logout compatibility with existing session management

6. **Update OAuth2 client registrations**
    - Add `openid` scope to default client scopes
    - Enable OIDC client metadata (response types, subject type, ID token signing algorithm)

**No Breaking Changes**: Existing OAuth2 clients continue working. OIDC features are opt-in via scope parameter.

## Capabilities

### New Capabilities

- `users-authentication`: OpenID Connect Core 1.0 authentication layer including ID tokens, discovery endpoint, UserInfo
  endpoint, and RP-initiated logout

### Modified Capabilities

- `users`: Extends existing OAuth2 authentication with ID token generation and OIDC claims. No requirement changes -
  adds optional OIDC functionality on top of existing OAuth2 flows.

## Impact

**Affected Code:**

- `AuthorizationServerConfiguration.java` - Enable OIDC settings
- `BootstrapDataLoader.java` - Add `openid` scope to default client
- JWT token customizer - Add OIDC claims for ID tokens

**Affected APIs:**

- New endpoint: `GET /.well-known/openid-configuration` (public)
- New endpoint: `GET /oauth2/userinfo` (authenticated with access token)
- New endpoint: `POST /oauth2/logout` (authenticated)
- Modified tokens: Authorization code flow can now return `id_token` parameter when requested

**Dependencies:**

- No new dependencies (Spring Authorization Server already includes OIDC support)
- Uses existing JWT signing infrastructure (RS256 with RSA keys)

**Systems:**

- Frontend: Can now use OIDC standard flows for authentication
- Database: Uses existing `oidc_id_token_*` columns (no schema changes)
- Testing: Requires new OIDC-specific test scenarios

**Compatibility:**

- Backwards compatible with existing OAuth2 clients
- Existing access tokens unchanged
- Frontend can progressively adopt OIDC features
