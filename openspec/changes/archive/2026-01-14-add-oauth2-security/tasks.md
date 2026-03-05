# Implementation Tasks

## 1. Domain Layer (Users)

- [x] 1.1 Create User aggregate root entity with invariants
- [x] 1.2 Create Role enum (ROLE_ADMIN, ROLE_MEMBER)
- [x] 1.3 Create AccountStatus enum (ACTIVE, PENDING_ACTIVATION, SUSPENDED)
- [x] 1.4 Create UserRepository interface
- [x] 1.5 Write User domain tests (creation, password change, authorities mapping)

## 2. Infrastructure Layer (Users)

- [x] 2.1 Create UserEntity JPA entity with mappings
- [x] 2.2 Create UserJpaRepository interface
- [x] 2.3 Create UserMapper for domain ↔ entity conversion
- [x] 2.4 Create UserRepositoryImpl implementing UserRepository
- [x] 2.5 Write repository integration tests with TestContainers

## 3. Database Migration

- [x] 3.1 Create V002__create_users_and_oauth2_tables.sql migration
- [x] 3.2 Add users table with columns (id, registration_number, password_hash, account_status, etc.)
- [x] 3.3 Add user_roles join table
- [x] 3.4 Add oauth2_registered_client table (Spring schema)
- [x] 3.5 Add oauth2_authorization table (Spring schema)
- [x] 3.6 Add oauth2_authorization_consent table (Spring schema)
- [x] 3.7 Insert bootstrap admin user (ZBM0001, BCrypt hash of "admin123", ROLE_ADMIN)
- [x] 3.8 Insert OAuth2 client registration (klabis-web, secret, scopes, grant types)
- [x] 3.9 Test migration with Flyway (clean database → migrate → verify schema)

## 4. Security Configuration

- [x] 4.1 Create SecurityConfiguration class with @EnableWebSecurity
- [x] 4.2 Configure authorizationServerSecurityFilterChain for OAuth2 endpoints
- [x] 4.3 Configure defaultSecurityFilterChain for API endpoints with authorization rules
- [x] 4.4 Add @EnableMethodSecurity for @PreAuthorize support
- [x] 4.5 Create PasswordEncoder bean (BCryptPasswordEncoder)
- [x] 4.6 Create JwtAuthenticationConverter bean (map JWT authorities claim)
- [x] 4.7 Write unit tests for security configuration
- [x] 4.8 Disable CSRF for stateless API
- [x] 4.9 Configure exception handling (401, 403)

## 5. Authorization Server Configuration

- [x] 5.1 Create AuthorizationServerConfiguration class
- [x] 5.2 Configure RegisteredClientRepository (JDBC-backed with JdbcRegisteredClientRepository)
- [x] 5.3 Configure OAuth2AuthorizationService (JDBC-backed with JdbcOAuth2AuthorizationService)
- [x] 5.4 Configure OAuth2AuthorizationConsentService (JDBC-backed)
- [x] 5.5 Create JWKSource bean (generate RSA key pair at startup)
- [x] 5.6 Create JwtDecoder bean (from JWKSource)
- [x] 5.7 Create OAuth2TokenCustomizer bean (add registrationNumber and authorities to JWT)
- [x] 5.8 Configure AuthorizationServerSettings (issuer, endpoints)
- [x] 5.9 Configure token settings (access token 15 min, refresh token 30 days, JWT format)

## 6. UserDetailsService Implementation

- [x] 6.1 Create KlabisUserDetails class implementing UserDetails
- [x] 6.2 Map User aggregate fields to UserDetails methods (getUsername, getPassword, getAuthorities)
- [x] 6.3 Create KlabisUserDetailsService implementing UserDetailsService
- [x] 6.4 Implement loadUserByUsername (registrationNumber) → query UserRepository
- [x] 6.5 Map User.roles to GrantedAuthority collection (ROLE_ADMIN → authorities)
- [x] 6.6 Write unit tests for KlabisUserDetailsService (user found, user not found, authorities mapping)

## 7. Controller Security

- [x] 7.1 Add @PreAuthorize("hasAuthority('MEMBERS:CREATE')") to MemberController.registerMember
- [x] 7.2 Add @PreAuthorize("hasAuthority('MEMBERS:READ')") to MemberController.getMember (future)
- [x] 7.3 Create SecurityExceptionHandler class with @RestControllerAdvice
- [x] 7.4 Handle AccessDeniedException → 403 Forbidden with ProblemDetail
- [x] 7.5 Handle AuthenticationException → 401 Unauthorized with ProblemDetail
- [x] 7.6 Write controller security tests with @WithMockUser (401, 403, 200 scenarios)

## 8. Auto-Provision User on Member Registration

- [x] 8.1 Create PasswordGenerator utility class (secure random password generation)
- [x] 8.2 Modify RegisterMemberCommandHandler to inject UserRepository
- [x] 8.3 Add @Transactional annotation to RegisterMemberCommandHandler.handle method
- [x] 8.4 After Member.create, call User.create with same registrationNumber
- [x] 8.5 Generate temporary password with PasswordGenerator
- [x] 8.6 Assign ROLE_MEMBER and PENDING_ACTIVATION status to new User
- [x] 8.7 Save User to userRepository in same transaction
- [x] 8.8 Write integration tests for Member+User creation (success, rollback on error, duplicate registrationNumber)

## 9. Audit Trail Integration

- [x] 9.1 Modify JpaAuditingConfiguration.getCurrentAuditor() to check SecurityContextHolder
- [x] 9.2 Extract registrationNumber from Authentication.getName() if authenticated
- [x] 9.3 Fallback to "system" if SecurityContext has no authentication
- [x] 9.4 Write tests for audit trail (authenticated → registrationNumber, unauthenticated → "system")

## 10. CORS Configuration

- [x] 10.1 Create CorsConfiguration class with @Configuration
- [x] 10.2 Register CorsFilter bean for frontend (http://localhost:3000)
- [x] 10.3 Allow credentials (cookies, Authorization header)
- [x] 10.4 Allow OAuth2 endpoints (/oauth2/*, /login, /logout)
- [x] 10.5 Configure allowed methods (GET, POST, PUT, DELETE, OPTIONS)

## 11. Testing

- [x] 11.1 Create SecurityTestBase abstract class with @SpringBootTest, @AutoConfigureMockMvc
- [x] 11.2 Write MemberControllerSecurityTest (POST /api/members without auth → 401)
- [x] 11.3 Write test: POST /api/members with wrong authority → 403
- [x] 11.4 Write test: POST /api/members with MEMBERS:CREATE → 201
- [x] 11.5 Write integration test for OAuth2 token issuance (password grant)
- [x] 11.6 Write integration test for JWT validation
- [x] 11.7 Update existing MemberControllerTest with @WithMockUser
- [x] 11.8 Verify all 36 existing tests still pass

## 12. Documentation

- [x] 12.1 Document OAuth2 flow in README.md (how to obtain token, how to call API)
- [x] 12.2 Add curl examples for token acquisition (password grant)
- [x] 12.3 Add curl examples for calling secured endpoints with Bearer token
- [x] 12.4 Document authority mapping (roles → authorities, scopes → authorities)
- [x] 12.5 Document bootstrap credentials (ZBM0001 / admin123) and CHANGE PASSWORD warning

## 13. Validation

- [x] 13.1 Run OpenSpec validation: `openspec validate add-oauth2-security --strict`
- [x] 13.2 Run all tests: `mvn test` - 78 tests PASSED
- [x] 13.3 Manual test: Start application and obtain token with curl
- [x] 13.4 Manual test: Call POST /api/members without token (expect 401)
- [x] 13.5 Manual test: Call POST /api/members with token (expect 201)
- [x] 13.6 Verify audit trail: Check created_by field in database
- [x] 13.7 Verify OAuth2 tables populated (clients, authorizations)
