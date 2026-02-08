# Change: Add OAuth2 Authentication and Authorization

## Why

The system currently has no authentication or authorization mechanism. The member registration API is publicly
accessible, and there is no way to:

- Restrict member creation to authorized users
- Provide login credentials for members to access their data
- Track which authenticated user performed an action (audit trail)
- Support future self-service features (member profile updates, event registration)

Spring Security and Spring Authorization Server dependencies are already in the project but not configured. This change
implements OAuth2 authentication and authorization to secure the API and enable proper access control.

## What Changes

- **New "auth" capability** with OAuth2 Authorization Server
    - User aggregate (separate from Member, linked via registrationNumber)
    - OAuth2 server with JWT access tokens (15 min) and opaque refresh tokens (30 days)
    - Role-based authorization: ROLE_ADMIN, ROLE_MEMBER
    - Permission-based access control: MEMBERS:CREATE, MEMBERS:READ, etc.

- **Database schema changes**
    - New tables: `users`, `user_roles`
    - OAuth2 tables: `oauth2_registered_client`, `oauth2_authorization`, `oauth2_authorization_consent`
    - Bootstrap data: Admin user (ZBM0001 / admin123), OAuth2 client (klabis-web)

- **Security infrastructure**
    - SecurityConfiguration: JWT validation, endpoint security
    - AuthorizationServerConfiguration: OAuth2 server beans
    - KlabisUserDetailsService: Load users by registrationNumber
    - OAuth2TokenCustomizer: Add custom claims (registrationNumber, authorities)

- **Auto-provision User on Member registration**
    - RegisterMemberCommandHandler creates both Member and User in transaction
    - Generate temporary password, send via email (future enhancement)
    - User assigned ROLE_MEMBER with PENDING_ACTIVATION status

- **Secure existing endpoints**
    - MemberController: Add @PreAuthorize("hasAuthority('MEMBERS:CREATE')") annotations
    - SecurityExceptionHandler: Handle 401/403 with ProblemDetail format

- **Audit trail integration**
    - JpaAuditingConfiguration: Use SecurityContext for created_by/modified_by
    - Fallback to "system" for unauthenticated operations

## Impact

- **Affected specs**:
    - `auth` (new capability)
    - `members` (modified - auto-create User, audit trail)

- **Affected code**:
    - **New domain**: `com.klabis.users.domain` (User, Role, AccountStatus, UserRepository)
    - **New infrastructure**: `com.klabis.users.infrastructure` (UserEntity, UserJpaRepository,
      KlabisUserDetailsService)
    - **New config**: `com.klabis.config` (SecurityConfiguration, AuthorizationServerConfiguration, CorsConfiguration,
      SecurityExceptionHandler)
    - **Modified**: `RegisterMemberCommandHandler` (auto-create User), `JpaAuditingConfiguration` (SecurityContext
      auditor), `MemberController` (@PreAuthorize)

- **Database**: V002 migration for users and OAuth2 tables

- **Testing**:
    - New security tests for 401/403 scenarios
    - Existing tests require @WithMockUser for authenticated operations
    - All 36 existing tests must still pass

- **Configuration**:
    - OAuth2 client credentials (klabis-web:secret)
    - RSA key pair for JWT signing (generated at startup)
    - CORS configuration for frontend (http://localhost:3000)

## Breaking Changes

None - this change adds authentication to a currently unsecured API. Existing endpoints remain functionally the same but
now require authentication.

## Out of Scope (Future Enhancements)

- Password reset flow via email
- Account activation via email link
- Multi-factor authentication (TOTP)
- Password complexity requirements
- Account lockout after failed attempts
- Token revocation/blacklist
- Social login (Google, Facebook)
