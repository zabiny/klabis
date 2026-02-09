# Implementation Tasks

## 1. Configuration - Add OIDC Scopes

- [ ] 1.1 Add "profile" and "email" scopes to BootstrapDataLoader OAuth2 client configuration
- [ ] 1.2 Verify bootstrap data loads with new scopes (check application logs on startup)

## 2. Core Implementation - UserInfo Mapper

- [ ] 2.1 Extract authorized scopes from OidcUserInfoAuthenticationContext in oidcUserInfoMapper()
- [ ] 2.2 Implement scope-based claim filtering logic (openid → only sub)
- [ ] 2.3 Replace custom claims (firstName, lastName) with OIDC standard claims (given_name, family_name)
- [ ] 2.4 Add profile scope claim mapping (given_name, family_name, registrationNumber, updated_at)
- [ ] 2.5 Add email scope claim mapping (email, email_verified)
- [ ] 2.6 Implement null-safe handling for Member without email (omit email claims)
- [ ] 2.7 Implement null-safe handling for admin users (omit all profile/email claims)

## 3. Testing - Unit Tests

- [ ] 3.1 Write test: openid scope only returns sub claim
- [ ] 3.2 Write test: profile scope returns given_name, family_name, registrationNumber, updated_at (with Member entity)
- [ ] 3.3 Write test: email scope returns email, email_verified (with Member entity and email)
- [ ] 3.4 Write test: email scope omits claims when Member has no email
- [ ] 3.5 Write test: admin user (no Member) returns only sub regardless of scopes
- [ ] 3.6 Write test: expired token returns 401 Unauthorized
- [ ] 3.7 Write test: missing Authorization header returns 401 Unauthorized
- [ ] 3.8 Write test: token without openid scope returns 403 Forbidden

## 4. Testing - Integration with Real Member

- [ ] 4.1 Create test Member with email using @Sql annotation
- [ ] 4.2 Test UserInfo endpoint with profile scope returns Member data
- [ ] 4.3 Test UserInfo endpoint with email scope returns Member email
- [ ] 4.4 Create test Member without email using @Sql annotation
- [ ] 4.5 Test UserInfo endpoint omits email claims when Member.email is null

## 5. Documentation Updates

- [ ] 5.1 Update openspec/specs/users-authentication/spec.md with delta spec (sync from change)
- [ ] 5.2 Update any API documentation or examples referencing UserInfo claims
- [ ] 5.3 Document breaking change (firstName/lastName → given_name/family_name) in changelog

## 6. Verification

- [ ] 6.1 Run full test suite and verify all tests pass
- [ ] 6.2 Start application and verify bootstrap data loads correctly
- [ ] 6.3 Manually test UserInfo endpoint with different scope combinations using .http file
- [ ] 6.4 Verify backward compatibility: existing openid-only requests still work
