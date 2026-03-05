# users Specification - Delta

## MODIFIED Requirements

### Requirement: User Aggregate

The User aggregate SHALL manage user accounts as a separate aggregate from members, linked via registrationNumber. When
a Member is created for a User, the User's UserId SHALL be used as the Member's UserId, establishing a direct identifier
relationship between the aggregates. Users cannot be deleted, only disabled via accountStatus changes. **The User
aggregate SHALL contain only identity-related data (credentials, account status) and SHALL NOT contain authorities or
permissions.**

**Rationale**: Authorities are now managed in separate UserPermissions aggregate to enable separation of authentication
and authorization concerns.

**Changed from previous version**: Removed authorities from User aggregate structure. Users are now created without
authorities - authorities are managed separately via UserPermissions aggregate.

#### Scenario: User created with credentials only

- **WHEN** a user is created with registrationNumber and password
- **THEN** the User aggregate is created with cryptographically hashed password
- **AND** the User has unique UserId (generated as new UUID)
- **AND** User accountStatus is set to specified value
- **AND** User can exist without linked Member
- **AND** User does NOT have authorities field (authorities managed separately)
- **AND** UserPermissions aggregate is created separately (may be empty initially)

#### Scenario: User password changed

- **WHEN** user password is changed with new password
- **THEN** password is cryptographically hashed and stored
- **AND** User identity (registrationNumber) and UserId are preserved
- **AND** linked Member (if exists) is not affected
- **AND** UserPermissions (if exists) is not affected

#### Scenario: User account suspended

- **WHEN** user account is suspended
- **THEN** accountStatus changed to SUSPENDED
- **AND** subsequent authentication attempts fail
- **AND** existing tokens remain valid until expiration
- **AND** linked Member (if exists) remains in database but user cannot access it

#### Scenario: Member created for user uses user UserId

- **WHEN** Member is created for an existing User
- **THEN** Member.id SHALL equal User.id (same UserId value)
- **AND** Member.registrationNumber SHALL equal User.registrationNumber
- **AND** User and Member remain separate aggregates with shared identifier

### Requirement: User Authentication

The system SHALL authenticate users via OAuth2 using registrationNumber as username and cryptographically hashed
password. **The access token SHALL contain authorities claims from UserPermissions aggregate, not from User entity.**

**Changed from previous version**: Authorities are now loaded from UserPermissions aggregate instead of User entity
during authentication.

#### Scenario: Successful authentication with valid credentials

- **WHEN** user submits valid registrationNumber and password to /oauth2/token
- **THEN** system returns access token (15 min TTL) and refresh token (30 day TTL)
- **AND** access token contains claims: registrationNumber, authorities (from UserPermissions), expiration

#### Scenario: Authentication fails with invalid credentials

- **WHEN** user submits invalid registrationNumber or password
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response includes error description "Invalid credentials"

#### Scenario: Token validation extracts user context

- **WHEN** API receives request with valid access token
- **THEN** system extracts registrationNumber and authorities from token claims
- **AND** SecurityContext is populated with authenticated user
- **AND** authorities are loaded from UserPermissions aggregate (not User entity)

#### Scenario: Expired access token rejected

- **WHEN** API receives request with expired access token
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response indicates token has expired

### Requirement: User Authorization

The system SHALL authorize API operations based on user authorities and OAuth2 scopes. **Authorization checks SHALL
query UserPermissions aggregate to determine user's direct authorities, and MAY consider group-based authorities in the
future.**

**Changed from previous version**: Authorization checks now query UserPermissions aggregate instead of User entity.
Authorization is now context-aware (actor, resourceOwner, requiredAuthority).

#### Scenario: User with required authority accesses endpoint

- **WHEN** authenticated user with MEMBERS:CREATE authority calls POST /api/members
- **THEN** authorization check passes (authority from UserPermissions)
- **AND** endpoint handler executes

#### Scenario: User without required authority denied access

- **WHEN** authenticated user without MEMBERS:CREATE authority calls POST /api/members
- **THEN** authorization check fails
- **AND** system returns HTTP 403 Forbidden with ProblemDetail

#### Scenario: Unauthenticated request denied

- **WHEN** unauthenticated request (no token) is made to secured endpoint
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response includes WWW-Authenticate header with Bearer challenge

## REMOVED Requirements

### Requirement: Authority Management in User Aggregate

**Reason**: This requirement has been removed because authorities are no longer managed in the User aggregate. Authority
management is now handled by the separate `user-permissions` capability with its own UserPermissions aggregate.

**Migration**: Use the new user-permissions API endpoints for all authority management operations. The User entity no
longer contains authorities field - all permission updates must go through UserPermissions aggregate.

#### Scenario: Update user authorities

- **REMOVED** - User entity no longer manages authorities. Use PUT /api/users/{id}/permissions via user-permissions
  capability instead.

#### Scenario: Get user authorities

- **REMOVED** - Use GET /api/users/{id}/permissions via user-permissions capability instead.

#### Scenario: Attempt to set empty authorities

- **REMOVED** - Validation now handled by user-permissions capability.

#### Scenario: User creation requires authorities

- **REMOVED** - Users are now created without authorities. UserPermissions is created separately and may be empty
  initially.

### Requirement: Permission Management Lockout Prevention Business Rule

**Reason**: This requirement has been removed from users capability because permission management business rules are now
enforced by the separate `user-permissions` capability.

**Migration**: Admin lockout prevention is now enforced by user-permissions capability when updating UserPermissions
aggregate.

#### Scenario: Query users with MEMBERS:PERMISSIONS before removing it

- **REMOVED** - Now handled by user-permissions capability's admin lockout prevention requirement.

#### Scenario: Prevent MEMBERS:PERMISSIONS removal from last user who has it

- **REMOVED** - Now handled by user-permissions capability's admin lockout prevention requirement.

#### Scenario: Allow MEMBERS:PERMISSIONS removal when multiple users have it

- **REMOVED** - Now handled by user-permissions capability's admin lockout prevention requirement.

#### Scenario: Users without MEMBERS:PERMISSIONS bypass lockout check

- **REMOVED** - Now handled by user-permissions capability's admin lockout prevention requirement.

#### Scenario: Keeping MEMBERS:PERMISSIONS bypasses lockout check

- **REMOVED** - Now handled by user-permissions capability's admin lockout prevention requirement.

### Requirement: Get User Permissions API

**Reason**: This requirement has been removed from users capability because permission queries are now handled by the
separate `user-permissions` capability.

**Migration**: Use GET /api/users/{id}/permissions endpoint provided by user-permissions capability.

#### Scenario: Get user permissions successfully

- **REMOVED** - Now handled by user-permissions capability's "Get User Permissions API" requirement.

#### Scenario: Unauthorized user attempts to get permissions

- **REMOVED** - Now handled by user-permissions capability's "Get User Permissions API" requirement.

#### Scenario: Get permissions for nonexistent user

- **REMOVED** - Now handled by user-permissions capability's "Get User Permissions API" requirement.

### Requirement: Update User Permissions API

**Reason**: This requirement has been removed from users capability because permission updates are now handled by the
separate `user-permissions` capability.

**Migration**: Use PUT /api/users/{id}/permissions endpoint provided by user-permissions capability.

#### Scenario: Update user permissions with valid authorities

- **REMOVED** - Now handled by user-permissions capability's "Update User Permissions API" requirement.

#### Scenario: Unauthorized user attempts to update permissions

- **REMOVED** - Now handled by user-permissions capability's "Update User Permissions API" requirement.

#### Scenario: Update nonexistent user's permissions

- **REMOVED** - Now handled by user-permissions capability's "Update User Permissions API" requirement.

#### Scenario: Invalid authority in request

- **REMOVED** - Now handled by user-permissions capability's "Update User Permissions API" requirement.

#### Scenario: Empty authorities list

- **REMOVED** - Now handled by user-permissions capability's "Update User Permissions API" requirement.

### Requirement: Prevent Permission Management Lockout

**Reason**: This requirement has been removed from users capability because admin lockout prevention is now enforced by
the separate `user-permissions` capability.

**Migration**: Admin lockout prevention is now handled by user-permissions capability's "Admin Lockout Prevention"
requirement.

#### Scenario: Attempt to remove MEMBERS:PERMISSIONS from last user who has it

- **REMOVED** - Now handled by user-permissions capability's "Admin Lockout Prevention" requirement.

#### Scenario: Multiple users with MEMBERS:PERMISSIONS exist - can remove from one

- **REMOVED** - Now handled by user-permissions capability's "Admin Lockout Prevention" requirement.

#### Scenario: User without MEMBERS:PERMISSIONS can have authorities modified freely

- **REMOVED** - Now handled by user-permissions capability's "Admin Lockout Prevention" requirement.

#### Scenario: Update user's authorities without removing MEMBERS:PERMISSIONS

- **REMOVED** - Now handled by user-permissions capability's "Admin Lockout Prevention" requirement.

### Requirement: Valid Authorities

**Reason**: This requirement has been removed from users capability because authority validation is now handled by the
separate `user-permissions` capability.

**Migration**: Authority validation is now provided by user-permissions capability's "Valid Authorities" requirement.

#### Scenario: Valid authorities list

- **REMOVED** - Now handled by user-permissions capability's "Valid Authorities" requirement.

#### Scenario: Authority validation error response

- **REMOVED** - Now handled by user-permissions capability's "Valid Authorities" requirement.
