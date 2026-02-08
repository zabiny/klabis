# users Specification Delta

## Change: remove-user-roles

This delta removes the roles concept from User aggregate, simplifying the permission model to use only authorities.

---

## MODIFIED Requirements

### Requirement: User Authorization

The system SHALL authorize API operations based on user authorities and OAuth2 scopes.

**Rationale**: Removed "derived from user roles" since authorities are now assigned directly, not derived from roles.

#### Scenario: User with required authority accesses endpoint

- **WHEN** authenticated user with MEMBERS:CREATE authority calls POST /api/members
- **THEN** authorization check passes
- **AND** endpoint handler executes

#### Scenario: User without required authority denied access

- **WHEN** authenticated user without MEMBERS:CREATE authority calls POST /api/members
- **THEN** authorization check fails
- **AND** system returns HTTP 403 Forbidden with ProblemDetail

#### Scenario: Unauthenticated request denied

- **WHEN** unauthenticated request (no token) is made to secured endpoint
- **THEN** system returns HTTP 401 Unauthorized
- **AND** response includes WWW-Authenticate header with Bearer challenge

---

### Requirement: User Aggregate

The system SHALL manage user accounts as a separate aggregate from members, linked via registrationNumber. When a Member
is created for a User, the User's UserId SHALL be used as the Member's UserId, establishing a direct identifier
relationship between the aggregates. Users cannot be deleted, only disabled via accountStatus changes.

**Rationale**: Removed roles from User aggregate structure. Users are now created with authorities only.

#### Scenario: User created with credentials and authorities

- **WHEN** User.create is called with registrationNumber, password, and authorities
- **THEN** User aggregate is created with BCrypt-hashed password
- **AND** User has unique UserId (generated as new UUID)
- **AND** User accountStatus is set to specified value
- **AND** User can exist without linked Member
- **AND** User has at least one authority assigned

#### Scenario: User password changed

- **WHEN** User.changePassword is called with new password
- **THEN** password is BCrypt-hashed and stored
- **AND** User identity (registrationNumber) and UserId are preserved
- **AND** linked Member (if exists) is not affected

#### Scenario: User account suspended

- **WHEN** User.suspend() is called
- **THEN** accountStatus changed to SUSPENDED
- **AND** subsequent authentication attempts fail
- **AND** existing tokens remain valid until expiration
- **AND** linked Member (if exists) remains in database but user cannot access it

#### Scenario: Member created for user uses user UserId

- **WHEN** Member is created for an existing User
- **THEN** Member.id SHALL equal User.id (same UserId value)
- **AND** Member.registrationNumber SHALL equal User.registrationNumber
- **AND** User and Member remain separate aggregates with shared identifier

---

## REMOVED Requirements

### Requirement: Role-Based Access Control

~~The system SHALL define roles that map to sets of authorities (permissions).~~

**Rationale**: Roles are removed from the system. All permissions are now managed via direct authority assignment. This
simplifies the domain model and eliminates confusion between "roles" and "authorities".

#### Scenario: ROLE_ADMIN has all permissions

~~- **WHEN** user has ROLE_ADMIN~~
~~- **THEN** user granted authorities: MEMBERS:CREATE, MEMBERS:READ, MEMBERS:UPDATE, MEMBERS:DELETE~~
~~- **AND** future authorities automatically granted to ROLE_ADMIN~~

**Replacement**: Authorities are assigned directly when creating users. Admin users receive all authorities: MEMBERS:
CREATE, MEMBERS:READ, MEMBERS:UPDATE, MEMBERS:DELETE, MEMBERS:PERMISSIONS.

#### Scenario: ROLE_MEMBER has limited permissions

~~- **WHEN** user has ROLE_MEMBER~~
~~- **THEN** user granted authority: MEMBERS:READ (own data only, future enforcement)~~

**Replacement**: Member users receive MEMBERS:READ authority when their User account is created during member
registration.

#### Scenario: Multiple roles aggregate authorities

~~- **WHEN** user has both ROLE_ADMIN and ROLE_MEMBER~~
~~- **THEN** user granted union of all authorities from both roles~~

**Replacement**: Not applicable. Users have a single set of authorities assigned directly.

---

## REMOVED Scenarios

### From Requirement: User Aggregate

#### Scenario: User authorities derived from roles

~~- **WHEN** User.getAuthorities() is called~~
~~- **THEN** system maps roles to authorities (ROLE_ADMIN → MEMBERS:CREATE, MEMBERS:READ, etc.)~~
~~- **AND** authorities returned as GrantedAuthority collection~~

**Rationale**: Authorities are no longer derived from roles. The User.getAuthorities() method returns the directly
assigned authorities set without any mapping or derivation.

**Replacement**: User.getAuthorities() returns the authorities directly assigned to the user without any
role-to-authority mapping.

---

## Implementation Notes

### Database Changes

- **Removed**: `user_roles` table
- **Unchanged**: `user_authorities` table continues to store user permissions

### Domain Model Changes

- **Removed**: `Role` enum (ROLE_ADMIN, ROLE_MEMBER)
- **Removed**: `UserRole` entity
- **Removed**: `Set<UserRole> roles` field from User aggregate
- **Removed**: `roles` field from UserCreatedEvent
- **Unchanged**: `Set<Authority> authorities` field remains in User aggregate

### API Impact

- **No breaking changes**: REST API does not expose roles field
- **Event impact**: UserCreatedEvent structure changes (roles field removed) - acceptable since application is not in
  production

### Migration Impact

- **No data migration needed**: Application uses in-memory H2 database
- **No production deployment**: Application is in development phase only

### Authority Assignment Examples

**Admin User** (via BootstrapDataLoader):

```java
authorities: ["MEMBERS:CREATE", "MEMBERS:READ", "MEMBERS:UPDATE", "MEMBERS:DELETE", "MEMBERS:PERMISSIONS"]
```

**Member User** (via RegisterMemberCommandHandler):

```java
authorities: ["MEMBERS:READ"]
```

### Valid Authorities Whitelist

Unchanged - defined in `AuthorityValidator.VALID_AUTHORITIES`:

- MEMBERS:CREATE
- MEMBERS:READ
- MEMBERS:UPDATE
- MEMBERS:DELETE
- MEMBERS:PERMISSIONS
