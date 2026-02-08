# auth Specification Delta

This specification extends authentication and authorization requirements to support permission management.

## ADDED Requirements

### Requirement: Authority Management in User Aggregate

The User aggregate SHALL support managing authorities as the sole source of user permissions, independent of roles.

#### Scenario: Update user authorities

- **WHEN** User.updateAuthorities(Set<String> newAuthorities) is called
- **AND** newAuthorities contains valid authority strings
- **AND** newAuthorities is not empty
- **THEN** User's authorities are replaced with the new set
- **AND** User's roles remain unchanged (roles are organizational labels only)
- **AND** new User instance is returned (immutable pattern)

#### Scenario: Get user authorities

- **WHEN** User.getAuthorities() is called
- **THEN** system returns the user's authorities
- **AND** authorities are never null or empty (business invariant)
- **AND** roles are NOT used to derive authorities

#### Scenario: Attempt to set empty authorities

- **WHEN** User.updateAuthorities(Set<String> newAuthorities) is called
- **AND** newAuthorities is empty or null
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates at least one authority is required

#### Scenario: User creation requires authorities

- **WHEN** User.create() is called
- **THEN** authorities parameter is required
- **AND** authorities must not be empty
- **AND** authorities are validated against allowed list

### Requirement: Permission Change Audit Logging

The system SHALL log all permission changes via @Auditable annotation for audit trail and compliance.

#### Scenario: Permission update triggers audit logging via @Auditable

- **WHEN** UpdateUserPermissionsCommandHandler.handle() method completes successfully
- **THEN** @Auditable annotation triggers AuditLogAspect
- **AND** audit log entry is created with:
    - Event type: USER_PERMISSIONS_CHANGED
    - Actor: authenticated user's registration number (from SecurityContext)
    - Target: user ID from command
    - IP address: extracted from HTTP request
    - Old value: previous authorities list
    - New value: updated authorities list
    - Timestamp
- **AND** audit entry is persisted in same transaction as permission update

#### Scenario: Failed permission update does not create audit entry

- **WHEN** UpdateUserPermissionsCommandHandler.handle() throws exception
- **THEN** @Auditable annotation does not create audit log entry
- **AND** existing permissions remain unchanged

### Requirement: Permission Management Lockout Prevention Business Rule

The system SHALL enforce that operations cannot remove MEMBERS:PERMISSIONS authority from all users.

#### Scenario: Query users with MEMBERS:PERMISSIONS before removing it

- **WHEN** system needs to validate permission management lockout prevention
- **THEN** repository provides method: countActiveUsersWithAuthority("MEMBERS:PERMISSIONS")
- **AND** count includes only users with accountStatus=ACTIVE

#### Scenario: Prevent MEMBERS:PERMISSIONS removal from last user who has it

- **WHEN** UpdateUserPermissionsCommandHandler validates permission update
- **AND** target user has MEMBERS:PERMISSIONS in current authorities
- **AND** new authorities do NOT include MEMBERS:PERMISSIONS
- **AND** count of active users with MEMBERS:PERMISSIONS returns 1 (last one)
- **THEN** validation fails
- **AND** CannotRemoveLastPermissionManagerException is thrown with descriptive message

#### Scenario: Allow MEMBERS:PERMISSIONS removal when multiple users have it

- **WHEN** UpdateUserPermissionsCommandHandler validates permission update
- **AND** target user has MEMBERS:PERMISSIONS
- **AND** new authorities do NOT include MEMBERS:PERMISSIONS
- **AND** count of active users with MEMBERS:PERMISSIONS returns 2 or more
- **THEN** validation passes
- **AND** operation proceeds

#### Scenario: Users without MEMBERS:PERMISSIONS bypass lockout check

- **WHEN** target user does not have MEMBERS:PERMISSIONS in current authorities
- **THEN** permission management lockout check is skipped
- **AND** operation proceeds based on other validations

#### Scenario: Keeping MEMBERS:PERMISSIONS bypasses lockout check

- **WHEN** target user has MEMBERS:PERMISSIONS in current authorities
- **AND** new authorities INCLUDE MEMBERS:PERMISSIONS
- **THEN** permission management lockout check is skipped (not removing it)
- **AND** operation proceeds

## MODIFIED Requirements

None (extends existing auth specification)

## REMOVED Requirements

None
