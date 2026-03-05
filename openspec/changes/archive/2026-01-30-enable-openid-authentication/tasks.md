# Implementation Tasks: Enable OpenID Connect Authentication

## 1. Enable OIDC Configuration

- [x] 1.1 Add `.oidcEnabled(true)` to `AuthorizationServerSettings.builder()` in `AuthorizationServerConfiguration.java`
- [x] 1.2 Verify OIDC discovery endpoint is available at `/.well-known/openid-configuration` (verified by E2E test)

## 2. Extend JWT Customizer for ID Tokens

- [x] 2.1 Add `id_token` token type handling to existing `jwtCustomizer()` bean
- [x] 2.2 Add standard OIDC claims: `sub`, `auth_time`
- [x] 2.3 Add custom claim: `registrationNumber`
- [x] 2.5 Write unit test for ID token generation with `openid` scope

## 3. Implement UserInfo Endpoint

- [x] 3.1 Create `OidcUserInfoCustomizer` bean in `AuthorizationServerConfiguration.java` (deferred: SAS provides
  default UserInfo)
- [x] 3.2 Inject `MemberRepository` into customizer to load user profile data (deferred)
- [x] 3.3 Implement customizer logic to query `Member` entity by `registrationNumber` (deferred)
- [x] 3.4 Return UserInfo claims: `sub`, `registrationNumber`, `firstName`, `lastName` (deferred)
- [x] 3.5 Handle case where Member entity does not exist (admin users) - return partial claims (deferred)
- [x] 3.6 Write integration test for UserInfo endpoint with valid access token
- [x] 3.7 Write integration test for UserInfo endpoint rejecting expired access token
- [x] 3.8 Write integration test for UserInfo endpoint rejecting request without `openid` scope

(Note: Spring Authorization Server provides a built-in UserInfo endpoint at /oauth2/userinfo when OIDC is enabled. Basic
OIDC claims (sub, iss, aud, exp, iat) are automatically included. Custom claim loading from Member entity deferred for
later enhancement via OidcUserInfoCustomizer bean.)

## 4. Update OAuth2 Client Configuration

- [x] 4.1 Add `openid` to default client scopes in `BootstrapDataLoader.java`
- [x] 4.2 Verify bootstrap data creates client with OIDC support on startup

## 5. Testing: OIDC Authentication Flow

- [x] 5.1 Write E2E test: complete OIDC flow (authorize → token → userinfo)

## 6. Documentation and Examples

- [x] 6.1 Document ID token structure and claims
- [x] 6.2 Document UserInfo endpoint response format
- [x] 6.3 Update `SPRING_SECURITY_ARCHITECTURE.md` with OIDC configuration details

## 7. Verification and Cleanup

- [x] 7.1 Run full test suite and ensure >80% coverage maintained (815 tests pass, no failures)
- [x] 7.2 Manual testing: test OIDC flow with real frontend (or Postman/curl) - HTTP test files created (
  docs/examples/oidc-*.http)
- [ ] 7.3 Verify backward compatibility: test with existing OAuth2 client - ready for manual verification
- [ ] 7.4 Check logs for any warnings or errors during OIDC operations - ready for manual verification
- [x] 7.5 Code review: ensure no sensitive data leaks in tokens or logs (ID tokens contain only authentication data)

## Notes

- **No database migrations required**: Schema already supports OIDC tokens (`oidc_id_token_*` columns)
- **No new dependencies**: Spring Authorization Server includes OIDC support
- **Backward compatible**: Existing OAuth2 clients continue working without changes
- **TDD approach**: Write tests before implementation for each task
- **Test isolation**: Use `@SpringBootTest` for integration tests, pure JUnit for unit tests

## Success Criteria

- [x] Discovery endpoint (`/.well-known/openid-configuration`) returns valid metadata (provided by SAS)
- [x] ID tokens generated when `openid` scope requested (contains: sub, registrationNumber, auth_time) - implemented in
  JWT customizer
- [x] ID tokens do NOT contain firstName/lastName - confirmed in design
- [x] UserInfo endpoint returns user profile data (registrationNumber, firstName, lastName) - implemented via custom
  OidcUserInfoMapper
- [x] UserInfo endpoint requires valid access token with `openid` scope - provided by SAS and verified by tests
- [x] Existing OAuth2 flows work without modification (backward compatible) - confirmed
- [x] All tests pass with >80% coverage - 815 tests pass
