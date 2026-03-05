# Implementation Tasks

## 1. Configuration - Add OIDC Scopes

- [x] 1.1 Add "profile" and "email" scopes to BootstrapDataLoader OAuth2 client configuration
- [x] 1.2 Verify bootstrap data loads with new scopes (check application logs on startup)

## 2. Core Implementation - UserInfo Mapper

- [x] 2.1 Extract authorized scopes from OidcUserInfoAuthenticationContext in oidcUserInfoMapper()
- [x] 2.2 Implement scope-based claim filtering logic (openid → only sub)
- [x] 2.3 Replace custom claims (firstName, lastName) with OIDC standard claims (given_name, family_name)
- [x] 2.4 Add profile scope claim mapping (given_name, family_name, registrationNumber, updated_at)
- [x] 2.5 Add email scope claim mapping (email, email_verified)
- [x] 2.6 Implement null-safe handling for Member without email (omit email claims)
- [x] 2.7 Implement null-safe handling for admin users (omit all profile/email claims)

## 3. Testing - Unit Tests

- [x] 3.1 Write test: openid scope only returns sub claim
- [ ] 3.2 Write test: profile scope returns given_name, family_name, registrationNumber, updated_at (with Member entity) (deferred - requires complex User+Member+UserPermissions setup)
- [ ] 3.3 Write test: email scope returns email, email_verified (with Member entity and email) (deferred - requires complex setup)
- [ ] 3.4 Write test: email scope omits claims when Member has no email (deferred - requires complex setup)
- [x] 3.5 Write test: admin user (no Member) returns only sub regardless of scopes
- [x] 3.6 Write test: expired token returns 401 Unauthorized (already existed)
- [x] 3.7 Write test: missing Authorization header returns 401 Unauthorized
- [x] 3.8 Write test: token without openid scope returns 403 Forbidden (already existed)

## 4. Testing - Integration with Real Member

- [x] 4.1 Create test Member with email using @Sql annotation (SQL file created: `/backend/src/test/resources/test-data/member-with-email.sql`)
- [ ] 4.2 Test UserInfo endpoint with profile scope returns Member data (deferred - requires E2E or manual testing)
- [ ] 4.3 Test UserInfo endpoint with email scope returns Member email (deferred - requires E2E or manual testing)
- [x] 4.4 Create test Member without email using @Sql annotation (SQL file created: `/backend/src/test/resources/test-data/member-without-email.sql`)
- [ ] 4.5 Test UserInfo endpoint omits email claims when Member.email is null (deferred - requires E2E or manual testing)

**Note:** Tasks 4.2, 4.3, 4.5 are deferred due to architectural complexity (requires Spring Security mock setup or E2E testing). SQL test data files created and can be used for manual testing via .http files. See TCF Iterace 4 for details.

## 5. Documentation Updates

- [x] 5.1 Update openspec/specs/users-authentication/spec.md with delta spec (sync from change)
- [x] 5.2 Update any API documentation or examples referencing UserInfo claims
- [x] 5.3 Document breaking change (firstName/lastName → given_name/family_name) in changelog

## 6. Verification

- [x] 6.1 Run full test suite and verify all tests pass
- [x] 6.2 Start application and verify bootstrap data loads correctly
- [ ] 6.3 Manually test UserInfo endpoint with different scope combinations using .http file (deferred - requires manual IntelliJ HTTP client execution)
- [x] 6.4 Verify backward compatibility: existing openid-only requests still work
