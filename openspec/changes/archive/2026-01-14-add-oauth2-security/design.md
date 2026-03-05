# OAuth2 Security Architecture Design

## Context

The Klabis backend needs authentication and authorization to secure the member registration API and future endpoints.
Spring Security and Spring Authorization Server are already dependencies but not configured. The system uses Clean
Architecture and DDD principles, so security implementation must respect bounded contexts and aggregate boundaries.

**Current state:**

- No authentication mechanism
- Member API endpoints publicly accessible
- JpaAuditingConfiguration returns hardcoded "system" auditor
- 36 passing tests (no security tests)

**Stakeholders:**

- Club administrators (need MEMBERS:CREATE permission)
- Club members (need login credentials for future self-service features)
- Frontend application (needs OAuth2 client to obtain tokens)

## Goals / Non-Goals

### Goals

- Implement OAuth2 authentication with Spring Authorization Server
- Create User aggregate separate from Member aggregate
- Auto-provision User accounts when Members are registered
- Secure Member API with role-based permissions
- Support JWT access tokens and opaque refresh tokens
- Enable audit trail with authenticated user context
- Maintain existing test coverage and API functionality

### Non-Goals

- Password reset flow (future enhancement)
- Email-based account activation (future enhancement)
- Multi-factor authentication (future enhancement)
- Social login providers (future enhancement)
- Separate authorization server microservice (over-engineering for current scale)
- Session-based authentication (stateless API required)

## Decisions

### Decision 1: User as Separate Aggregate

**What:** User and Member are separate aggregates in different bounded contexts (Identity vs Domain).

**Why:**

- **Separation of concerns**: Authentication/authorization is orthogonal to member domain logic
- **Different lifecycles**: User can be suspended without deleting Member record
- **Every Member has a User**, but **not every User has a Member** (e.g., admin user, future external users)
- **Link via registrationNumber**: Natural key, nullable foreign key on User side
- **DDD compliance**: Each aggregate has single responsibility, clear boundaries

**Alternatives considered:**

1. **Embed authentication in Member aggregate**
    - Rejected: Violates Single Responsibility Principle
    - Rejected: Couples security concerns to domain entity
    - Rejected: Makes Member aggregate more complex

2. **Create UserMember join aggregate**
    - Rejected: Unnecessary complexity, no business invariants to enforce
    - Rejected: registrationNumber link is sufficient

**Trade-offs:**

- ✅ Clean separation, easy to extend User without touching Member
- ✅ Admin users don't need fake Member records
- ❌ Two repositories needed for Member+User operations
- ❌ Must coordinate transaction across aggregates (handled in command handler)

### Decision 2: Hybrid OAuth2 Role (Authorization Server + Resource Server)

**What:** Combine Authorization Server and Resource Server in single Spring Boot application.

**Why:**

- **Club scale**: <1000 members, not microservices complexity
- **Simplicity**: Single deployment, no network latency between auth and API
- **Development velocity**: Faster iteration, easier debugging, fewer moving parts
- **Standard pattern**: Spring Authorization Server explicitly supports hybrid mode
- **Cost effective**: No additional infrastructure for separate auth service

**Alternatives considered:**

1. **Separate Authorization Server microservice**
    - Rejected: Over-engineering for current scale
    - Rejected: Increases operational complexity (2 services, network calls, distributed transactions)
    - Rejected: Not required until multi-service architecture

2. **Third-party OAuth2 provider (Auth0, Okta)**
    - Rejected: Vendor lock-in, recurring costs
    - Rejected: Custom User↔Member relationship harder to implement
    - Rejected: registrationNumber as username requires custom user store

**Trade-offs:**

- ✅ Simple deployment, single codebase
- ✅ No network latency for token validation (local JWT verification)
- ❌ Cannot scale auth independently of API (not a concern at current scale)
- ❌ If multiple services added later, may need to extract auth server (acceptable future refactor)

### Decision 3: JWT Access Tokens + Opaque Refresh Tokens

**What:**

- Access tokens: JWT format, 15 minute TTL, self-contained
- Refresh tokens: Opaque (random string), 30 day TTL, stored in database

**Why:**

- **Stateless validation**: Resource server validates JWT without database lookup (performance)
- **Custom claims**: Include registrationNumber and authorities in JWT for audit trail
- **Security**: Short-lived access tokens limit damage from token leakage
- **Revocable refresh tokens**: Database storage allows immediate revocation
- **Standard practice**: Common OAuth2 pattern for stateless APIs

**Alternatives considered:**

1. **Opaque access tokens (database lookup on every request)**
    - Rejected: Performance overhead, database hit on every API call
    - Rejected: Doesn't scale well (token introspection bottleneck)

2. **Long-lived JWT access tokens (no refresh tokens)**
    - Rejected: Cannot revoke JWT before expiration (security risk)
    - Rejected: Requires user to re-authenticate frequently (poor UX)

**Trade-offs:**

- ✅ High performance (no DB lookup for access token validation)
- ✅ Audit trail includes registrationNumber from token claims
- ❌ Cannot revoke JWT before expiration (mitigation: short 15 min TTL)
- ❌ Token size larger than opaque (acceptable, ~1KB for JWT)

**Future enhancement:** Add token blacklist for critical revocations (user suspension, password change).

### Decision 4: Auto-Create User on Member Registration

**What:** RegisterMemberCommandHandler creates both Member and User in single @Transactional method.

**Why:**

- **Consistency**: Every member immediately has login credentials
- **Atomic operation**: Both created or both rolled back (no orphaned records)
- **Simplicity**: No separate user provisioning workflow
- **DDD pattern**: Command handler coordinates cross-aggregate operation
- **Business requirement**: Members need system access upon registration

**Implementation:**

```java
@Transactional
public UUID handle(RegisterMemberCommand command) {
    // Generate registration number
    RegistrationNumber regNum = registrationNumberGenerator.generate(...);

    // Create Member aggregate
    Member member = Member.create(regNum, ...);
    Member savedMember = memberRepository.save(member);

    // Auto-create User aggregate
    String tempPassword = passwordGenerator.generate();
    User user = User.create(regNum, tempPassword, Set.of(Role.ROLE_MEMBER));
    userRepository.save(user);

    // TODO: Send welcome email with activation link

    return savedMember.getId();
}
```

**Alternatives considered:**

1. **Separate user creation endpoint**
    - Rejected: Requires admin to create user manually after member registration
    - Rejected: Two-step process error-prone (member created, user forgotten)

2. **Event-driven user creation (MemberCreatedEvent → UserCreatedEventHandler)**
    - Rejected: Eventual consistency risk (member exists but no user yet)
    - Rejected: More complex error handling (event handler failure requires retry logic)
    - Considered for future: Event can trigger welcome email asynchronously

**Trade-offs:**

- ✅ Atomic, consistent, simple
- ✅ No chance of member without user
- ❌ Command handler has dependency on UserRepository (cross-aggregate coordination)
- ❌ Couples Member and User lifecycles (acceptable for this business requirement)

### Decision 5: Authority Mapping Strategy

**What:** Map Roles and OAuth2 Scopes to fine-grained authorities (permissions).

**Mapping:**

```
ROLE_ADMIN:
  - MEMBERS:CREATE
  - MEMBERS:READ
  - MEMBERS:UPDATE
  - MEMBERS:DELETE
  - (all future authorities)

ROLE_MEMBER:
  - MEMBERS:READ (own data only, future)
  - EVENTS:READ
  - EVENTS:REGISTER

OAuth2 Scope "members.write":
  - MEMBERS:CREATE
  - MEMBERS:UPDATE
  - MEMBERS:DELETE

OAuth2 Scope "members.read":
  - MEMBERS:READ
```

**Why:**

- **Fine-grained control**: Permissions at operation level (CREATE, READ, UPDATE, DELETE)
- **Flexible**: Can grant scope to client without granting role to user
- **Extensible**: Easy to add new permissions (EVENTS:*, FINANCES:*) without changing roles
- **Standard practice**: Spring Security @PreAuthorize works with authorities
- **OAuth2 compliance**: Scopes limit what client application can do on user's behalf

**Alternatives considered:**

1. **Role-based only (no authorities)**
    - Rejected: Coarse-grained, cannot limit permissions without creating many roles
    - Rejected: Cannot limit OAuth2 client independently of user role

2. **Permission-based only (no roles)**
    - Rejected: Must assign many individual permissions to each user (tedious)
    - Rejected: Roles provide convenient grouping

**Trade-offs:**

- ✅ Fine-grained, flexible, extensible
- ✅ OAuth2 scopes limit client capabilities
- ❌ More complex (roles + scopes + authorities mapping logic)
- ❌ Must maintain mapping in OAuth2TokenCustomizer and KlabisUserDetails

### Decision 6: Bootstrap Admin User (ZBM0001)

**What:** V002 database migration inserts admin user with:

- registrationNumber: ZBM0001 (club code + birth year 00 + sequence 01)
- password: BCrypt hash of "admin123"
- roles: ROLE_ADMIN
- accountStatus: ACTIVE
- **No linked Member** (registrationNumber not in members table)

**Why:**

- **Chicken-and-egg**: Need authenticated user to create first member via API
- **Validates separation**: Proves User can exist without Member
- **Immediate usability**: System ready to use after migration
- **Admin not domain member**: Admin user manages system but may not be club member

**Alternatives considered:**

1. **Create admin via CLI command after migration**
    - Rejected: Extra manual step, error-prone in deployment
    - Rejected: Requires additional tooling

2. **Create admin member first, then admin user**
    - Rejected: Chicken-and-egg (how to create member without authentication?)
    - Rejected: Forces admin to be a member (not always true)

**Trade-offs:**

- ✅ System immediately usable after migration
- ✅ Validates User/Member separation
- ❌ Default password in migration script (security risk if not changed)
- **Mitigation**: Document that password MUST be changed on first login (future: force password change)

## Risks / Trade-offs

### Risk: JWT Token Cannot Be Revoked Before Expiration

**Impact:** If user account suspended or password changed, JWT valid until expiration.

**Mitigation:**

- Short access token TTL (15 minutes) limits exposure window
- Refresh tokens stored in database, can be immediately revoked
- Future enhancement: Add token blacklist for critical revocations

**Probability:** Medium (account suspension is rare but critical)
**Severity:** Medium (15 min window of unauthorized access)

### Risk: Default Admin Password in Migration

**Impact:** If admin password not changed, security vulnerability.

**Mitigation:**

- Document clearly in migration script and README: CHANGE PASSWORD ON FIRST LOGIN
- Future enhancement: Force password change on first login (check User.passwordChangedAt == null)

**Probability:** Medium (human error, forgot to change)
**Severity:** High (full admin access)

### Risk: Breaking Existing Tests

**Impact:** 36 existing tests may fail if they call authenticated endpoints without credentials.

**Mitigation:**

- Add @WithMockUser to tests that call secured endpoints
- Create SecurityTestBase with common test setup
- Run all tests after each phase to catch breakage early
- Document test changes in tasks.md

**Probability:** High (security changes affect all tests)
**Severity:** Low (tests can be fixed with @WithMockUser annotation)

### Risk: User-Member Consistency (Transaction Rollback)

**Impact:** If Member creation succeeds but User creation fails, inconsistent state.

**Mitigation:**

- Use @Transactional on RegisterMemberCommandHandler (atomic operation)
- Database unique constraint on registrationNumber (prevents duplicates)
- Write integration tests for rollback scenarios
- Both saves in same transaction (both succeed or both roll back)

**Probability:** Low (framework handles transaction correctly)
**Severity:** Medium (orphaned member without user)

### Risk: RSA Key Pair Generated at Startup

**Impact:** JWT signing key changes on every restart, invalidating all existing tokens.

**Mitigation:**

- Short token TTL (15 min) means tokens expire quickly anyway
- Document in README: "Restart invalidates tokens, users must re-authenticate"
- Future enhancement: Externalize key pair to file or key management service

**Probability:** High (key changes on restart)
**Severity:** Low (users re-authenticate, minor inconvenience)

## Migration Plan

### Phase 1: Foundation (No Breaking Changes)

1. Create User domain and infrastructure (no integration yet)
2. Create database migration V002 (new tables, no changes to existing)
3. Create security configurations (disabled by default)
4. All existing tests still pass (no authentication required yet)

### Phase 2: Integration (Breaking Changes for Tests)

1. Enable security configurations
2. Add @PreAuthorize to MemberController
3. Update JpaAuditingConfiguration to use SecurityContext
4. Update tests with @WithMockUser
5. Verify all tests pass with authentication

### Phase 3: Auto-Provisioning

1. Modify RegisterMemberCommandHandler to create User
2. Add @Transactional for atomic operation
3. Write integration tests for Member+User creation

### Phase 4: Validation

1. Manual testing with curl (token acquisition, API calls)
2. Integration tests for 401/403 scenarios
3. Verify audit trail (created_by populated from token)
4. OpenSpec validation

### Rollback Plan

- If critical issue found after deployment:
    - Disable security by setting `spring.security.enabled=false` (requires code change)
    - Redeploy previous version
    - Investigate issue, fix, redeploy
- Database migration rollback:
    - V002 down migration drops users and OAuth2 tables
    - Member data unaffected (no foreign keys to users)

## Open Questions

- ~~Should admin user have a linked Member record?~~ **RESOLVED:** No, validates User/Member separation
- ~~JWT or opaque access tokens?~~ **RESOLVED:** JWT for performance
- ~~How to handle token revocation?~~ **RESOLVED:** Short TTL + refresh token revocation, future blacklist
- ~~Where to store RSA key pair?~~ **RESOLVED:** Generate at startup for now, externalize later
- **Email service for password reset:** Which provider? (SMTP, SendGrid, AWS SES) - DEFERRED to future change
- **Password complexity requirements:** Min length? Special characters? - DEFERRED to future change
- **Account lockout policy:** How many failed attempts? Duration? - DEFERRED to future change

## References

- Spring Authorization Server documentation: https://docs.spring.io/spring-authorization-server/reference/
- Spring Security OAuth2 Resource
  Server: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/
- RFC 6749 (OAuth 2.0): https://datatracker.ietf.org/doc/html/rfc6749
- RFC 7519 (JWT): https://datatracker.ietf.org/doc/html/rfc7519
- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
