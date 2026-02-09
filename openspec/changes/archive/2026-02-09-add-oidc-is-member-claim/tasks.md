## 1. Update OIDC UserInfo Mapper

- [x] 1.1 Modify `oidcUserInfoMapper()` to add `user_name` claim when profile scope is present
- [x] 1.2 Modify `oidcUserInfoMapper()` to add `is_member` claim when profile scope is present
- [x] 1.3 Update logic to set `is_member: true` when Member aggregate exists
- [x] 1.4 Update logic to set `is_member: false` when Member aggregate does not exist or username format is not registration number
- [x] 1.5 Remove `registrationNumber` claim from `addProfileClaims()` helper method (now added by mapper)

## 2. Update JWT Token Customizer

- [x] 2.1 Rename `registrationNumber` claim to `user_name` in ID token generation (`jwtCustomizer()`)
- [x] 2.2 Rename `registrationNumber` claim to `user_name` in access token generation (`jwtCustomizer()`)
- [x] 2.3 Update JavaDoc comments to reflect new claim name

## 3. Update Tests

- [x] 3.1 Update `OidcUserInfoEndpointTest.shouldReturnUserInfoWithValidAccessToken()` - expect no `user_name`/`is_member` without profile scope
- [x] 3.2 Update `OidcUserInfoEndpointTest.shouldReturnOnlySubClaimWithOpenidScopeOnly()` - expect no `user_name`/`is_member` claims
- [x] 3.3 Rename test `shouldReturnOnlySubForAdminUserRegardlessOfScopes()` to `shouldReturnIsMemberFalseForAdminUserWithProfileScope()`
- [x] 3.4 Update test to expect `user_name: "admin"` and `is_member: false` for admin user with profile scope
- [x] 3.5 Update `OidcFlowE2ETest` - change assertion from `registrationNumber` to `user_name` in ID token
- [x] 3.6 Fix `OidcIdTokenGenerationTest` - add Members mock parameter to `jwtCustomizer()` call
- [x] 3.7 Run all tests to verify changes (`./gradlew test`)

## 4. Update Documentation

- [x] 4.1 Update `docs/examples/oidc-authentication.http` - update UserInfo endpoint comments
- [x] 4.2 Add example response for member user (with `is_member: true`)
- [x] 4.3 Add example response for admin user (with `is_member: false`)
- [x] 4.4 Update claim descriptions in comments (registrationNumber → user_name)

## 5. Verification

- [x] 5.1 All tests pass (1055 tests completed)
- [x] 5.2 Manual verification - code review completed
- [x] 5.3 Breaking changes documented in proposal.md

## Notes

All tasks marked as completed ✓ - implementation was done during exploration phase.
This change is **BREAKING** for frontend clients consuming OIDC userinfo endpoint.
Frontend must update from `registrationNumber` to `user_name` claim.
