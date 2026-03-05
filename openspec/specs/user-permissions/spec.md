# user-permissions Specification

## Purpose

This specification defines requirements for user permissions management including:

- Direct authority assignment and revocation
- Authorization checks based on context (actor, resource owner, required authority)
- Support for both global authorities and context-specific authorities
- Admin lockout prevention
- Extensibility for future group-based delegated authorization

This capability separates permission management from user identity, allowing the authorization model to scale
independently from authentication concerns.

## Requirements

### Requirement: User Permissions Aggregate

The system SHALL manage user permissions as a separate aggregate from user identity, containing only direct authorities
assigned to the user.

**Rationale**: Separating permissions from identity allows authorization to scale independently and enables contextual
authorization checks.

#### Scenario: UserPermissions created for user

- **WHEN** a UserPermissions aggregate is created for a user
- **THEN** the aggregate contains the user's UserId
- **AND** the aggregate contains set of direct authorities
- **AND** the aggregate is persisted separately from User entity

#### Scenario: UserPermissions defaults to empty authorities

- **WHEN** a UserPermissions aggregate is created without specifying authorities
- **THEN** the aggregate is created with empty authorities set
- **AND** no default authorities are assigned

### Requirement: Grant Direct Authority

The system SHALL allow granting direct authorities to a user, subject to validation and business rules.

#### Scenario: Grant authority to user successfully

- **WHEN** user with MEMBERS:PERMISSIONS authority grants authority to target user
- **AND** the authority is valid
- **AND** the operation does not violate admin lockout prevention
- **THEN** the authority is added to user's direct authorities
- **AND** the UserPermissions aggregate is updated

#### Scenario: Grant duplicate authority idempotent

- **WHEN** user grants authority that user already has
- **THEN** operation succeeds (idempotent)
- **AND** no duplicate authority is added

#### Scenario: Grant global authority enforces scope validation

- **WHEN** user attempts to grant authority to user via group mechanism
- **AND** the authority is a global authority (e.g., MEMBERS:PERMISSIONS, SYSTEM:ADMIN)
- **THEN** validation fails
- **AND** error message indicates global authorities cannot be granted via groups
- **AND** global authorities must be granted directly

### Requirement: Revoke Direct Authority

The system SHALL allow revoking direct authorities from a user, subject to admin lockout prevention.

#### Scenario: Revoke authority successfully

- **WHEN** user with MEMBERS:PERMISSIONS authority revokes authority from target user
- **AND** the operation does not violate admin lockout prevention
- **THEN** the authority is removed from user's direct authorities
- **AND** the UserPermissions aggregate is updated

#### Scenario: Revoke non-existent authority idempotent

- **WHEN** user revokes authority that user does not have
- **THEN** operation succeeds (idempotent)
- **AND** no error is raised

### Requirement: Authority Scope Classification

The system SHALL classify authorities as either global (not group-grantable) or context-specific (group-grantable).

#### Scenario: Global authorities cannot be granted via groups

- **GIVEN** an authority is classified as global (e.g., MEMBERS:PERMISSIONS, SYSTEM:ADMIN)
- **WHEN** system validates group authorization grant
- **AND** the group attempts to grant this global authority
- **THEN** validation fails
- **AND** error indicates global authorities must be granted directly

#### Scenario: Context-specific authorities can be granted via groups

- **GIVEN** an authority is classified as context-specific (e.g., TRAINING:VIEW, MEMBERSHIP:MANAGE)
- **WHEN** system validates group authorization grant
- **AND** the group attempts to grant this authority
- **THEN** validation succeeds
- **AND** authority can be granted via group mechanism

### Requirement: Authorization Context

The system SHALL support authorization checks based on context consisting of actor, resource owner, and required
authority.

**Rationale**: Contextual authorization enables future group-based delegation where permissions depend on whose data is
being accessed.

#### Scenario: AuthorizationContext captures actor, resourceOwner, and requiredAuthority

- **WHEN** an authorization check is performed
- **THEN** context includes actor (who is making the request)
- **AND** context includes resourceOwner (whose data/resource is being accessed)
- **AND** context includes requiredAuthority (what authority is needed)

#### Scenario: Self-access context has actor equal to resourceOwner

- **WHEN** user accesses their own data
- **THEN** AuthorizationContext has actor equal to resourceOwner
- **AND** authorization check proceeds normally (no special self-access rule)

### Requirement: Authorization Query

The system SHALL query both direct authorities and (future) group-based authorities when performing authorization
checks.

#### Scenario: Check authorization with direct authority

- **GIVEN** user has direct authority MEMBERS:READ
- **WHEN** authorization check is performed for context requiring MEMBERS:READ
- **THEN** check returns true (authorized)

#### Scenario: Check authorization without direct authority

- **GIVEN** user does not have direct authority MEMBERS:READ
- **AND** user is not member of any group that grants MEMBERS:READ
- **WHEN** authorization check is performed for context requiring MEMBERS:READ
- **THEN** check returns false (not authorized)

#### Scenario: Authorization check does not grant automatic self-access

- **GIVEN** user does not have MEMBERSHIP:EDIT_PERSONAL authority
- **WHEN** user attempts to edit their own firstName
- **THEN** authorization check returns false
- **AND** user cannot edit own data without required authority

#### Scenario: Authorization check for self-access with authority

- **GIVEN** user has MEMBERSHIP:VIEW authority
- **WHEN** user views their own profile
- **THEN** authorization check returns true
- **AND** user can view own profile

### Requirement: Admin Lockout Prevention

The system SHALL prevent removal of MEMBERS:PERMISSIONS authority from the last user who has it.

#### Scenario: Prevent removing MEMBERS:PERMISSIONS from last admin

- **WHEN** user attempts to revoke MEMBERS:PERMISSIONS authority
- **AND** target user currently has MEMBERS:PERMISSIONS
- **AND** only one active user has MEMBERS:PERMISSIONS in the system
- **THEN** operation fails
- **AND** error indicates last permission manager cannot be removed
- **AND** HTTP 409 Conflict is returned

#### Scenario: Allow removing MEMBERS:PERMISSIONS when multiple admins exist

- **WHEN** user attempts to revoke MEMBERS:PERMISSIONS authority
- **AND** target user currently has MEMBERS:PERMISSIONS
- **AND** two or more active users have MEMBERS:PERMISSIONS
- **THEN** operation succeeds
- **AND** authority is revoked

#### Scenario: Count active users with specific authority

- **WHEN** system needs to validate admin lockout prevention
- **THEN** repository provides method to count active users with specific authority
- **AND** count includes only users with account status ACTIVE
- **AND** count considers only direct authorities (not group-based)

### Requirement: Get User Permissions API

The system SHALL provide REST API endpoint for retrieving a user's direct authorities with proper authorization.

#### Scenario: Get user permissions successfully

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits GET request to /api/users/{id}/permissions
- **AND** target user exists
- **THEN** HTTP 200 OK is returned
- **AND** response includes user ID
- **AND** response includes list of direct authorities
- **AND** response does NOT include group-based authorities (implementation detail)
- **AND** response includes HATEOAS links

#### Scenario: Get permissions returns only direct authorities

- **GIVEN** user has direct authorities {MEMBERS:READ}
- **AND** user is member of groups that grant additional authorities
- **WHEN** GET /api/users/{id}/permissions is called
- **THEN** response includes only direct authorities {MEMBERS:READ}
- **AND** group-based authorities are NOT included in response
- **AND** effective authorization (direct + group) is evaluated at access time

#### Scenario: Unauthorized user cannot get permissions

- **WHEN** authenticated user without MEMBERS:PERMISSIONS authority attempts to get permissions
- **THEN** HTTP 403 Forbidden is returned
- **AND** error message indicates missing authority

#### Scenario: Get permissions for nonexistent user

- **WHEN** user with MEMBERS:PERMISSIONS authority requests permissions for nonexistent user ID
- **THEN** HTTP 404 Not Found is returned
- **AND** error message indicates user not found

### Requirement: Update User Permissions API

The system SHALL provide REST API endpoint for updating a user's direct authorities with proper authorization and
validation.

#### Scenario: Update user permissions successfully

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits PUT request to /api/users/{id}/permissions
- **AND** request body contains valid authority list
- **AND** operation does not violate admin lockout rule
- **THEN** system replaces user's direct authorities with provided list
- **AND** HTTP 200 OK is returned
- **AND** response includes user ID and updated authorities
- **AND** response includes HATEOAS links

#### Scenario: Update permissions with empty list rejected

- **WHEN** authenticated user submits request with empty authorities list []
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates at least one authority required
- **AND** existing permissions remain unchanged

#### Scenario: Update permissions with invalid authority

- **WHEN** request contains invalid authority string
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates which authority is invalid
- **AND** error message lists valid authorities

#### Scenario: Update permissions violates admin lockout

- **WHEN** request would remove MEMBERS:PERMISSIONS from last admin user
- **THEN** HTTP 409 Conflict is returned
- **AND** error message indicates operation would remove last permission manager
- **AND** existing permissions remain unchanged

### Requirement: Extensibility for Group-Based Authorization

The system SHALL be designed to support future group-based delegated authorization without breaking existing direct
authority functionality.

**Rationale**: Member groups feature will allow group admins to receive group authorities when accessing group members'
data.

#### Scenario: Authorization check supports future group integration

- **GIVEN** authorization check is performed
- **AND** groups feature does not exist yet
- **WHEN** check is executed
- **THEN** check evaluates direct authorities
- **AND** check design allows extension for group-based authorities
- **AND** no breaking changes to existing authorization logic

#### Scenario: Group-based authorization is not implemented yet

- **GIVEN** groups feature is not yet developed
- **WHEN** authorization check is performed
- **THEN** only direct authorities are evaluated
- **AND** group membership is not checked
- **AND** system behavior is well-defined for current state

### Requirement: Valid Authorities

The system SHALL restrict permission updates to a predefined set of valid authorities.

#### Scenario: Valid authorities list

- **GIVEN** the system defines valid authorities:
    - Global authorities: MEMBERS:PERMISSIONS, SYSTEM:ADMIN
    - Context-specific: TRAINING:*, TRAINING:VIEW, TRAINING:MANAGE
    - Context-specific: COMPETITIONS:*, COMPETITIONS:VIEW, COMPETITIONS:MANAGE
    - Context-specific: MEMBERSHIP:*, MEMBERSHIP:VIEW, MEMBERSHIP:MANAGE
- **WHEN** user requests to set permissions with any valid authority
- **THEN** validation passes

#### Scenario: Invalid authority rejected with helpful error

- **WHEN** request includes invalid authority
- **THEN** error response includes complete list of valid authorities
- **AND** error message clearly identifies which authority was invalid
