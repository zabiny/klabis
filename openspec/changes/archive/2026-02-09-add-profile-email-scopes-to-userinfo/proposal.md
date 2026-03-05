## Why

The current OIDC UserInfo endpoint returns all user profile claims (firstName, lastName, registrationNumber) under the basic `openid` scope, which violates OIDC specification and prevents granular control over personal data access. This change implements proper OIDC scope-based access control using standard `profile` and `email` scopes, improving GDPR compliance and security.

## What Changes

- **Add `profile` scope** to OAuth2 client configuration for accessing user profile claims (given_name, family_name, registrationNumber, updated_at)
- **Add `email` scope** to OAuth2 client configuration for accessing email claims (email, email_verified)
- **Replace custom claims with OIDC standard claims:**
  - `firstName` → `given_name` (OIDC standard)
  - `lastName` → `family_name` (OIDC standard)
  - Add `email` claim (OIDC standard)
  - Add `email_verified` claim (OIDC standard, initially false)
  - Add `updated_at` claim (OIDC standard, profile last modification timestamp)
- **Implement scope-based claim filtering** in UserInfo endpoint mapper:
  - `openid` scope alone returns only `sub` (subject identifier)
  - `profile` scope required for given_name, family_name, registrationNumber
  - `email` scope required for email, email_verified
- **Move registrationNumber to profile scope** - only returned when profile scope is present and Member entity exists
- **Omit claims when data unavailable** (OIDC-compliant):
  - No Member entity → omit all profile/email claims
  - Member without email → omit email claims
  - Admin users → return only `sub`

**BREAKING**: Removes custom claims `firstName` and `lastName` from UserInfo response (replaced with standard `given_name` and `family_name`). Since no frontend currently consumes UserInfo endpoint, this has no impact.

## Capabilities

### New Capabilities
<!-- None - this modifies existing authentication capabilities -->

### Modified Capabilities

- `users-authentication`: Changes UserInfo endpoint requirements to use OIDC standard scopes (`profile`, `email`) and standard claims (`given_name`, `family_name`, `email`, `email_verified`) instead of custom claims. Adds scope-based claim filtering and proper null handling.

## Impact

**Configuration:**
- `BootstrapDataLoader.java` - add "profile" and "email" to default OAuth2 client scopes

**Business Logic:**
- `AuthorizationServerConfiguration.oidcUserInfoMapper()` - implement scope-based claim filtering, replace custom claims with OIDC standard claims, add email support

**Testing:**
- `OidcUserInfoEndpointTest.java` - update tests for new claims structure, add tests for profile/email scopes, add tests for Member with/without email

**Specifications:**
- `openspec/specs/users-authentication/spec.md` - update UserInfo endpoint requirement with new scope behavior and standard claims

**Database/API:**
- No database schema changes
- No public API endpoint changes (same `/oauth2/userinfo` endpoint)
- Response schema changes (claim names and scope requirements)
