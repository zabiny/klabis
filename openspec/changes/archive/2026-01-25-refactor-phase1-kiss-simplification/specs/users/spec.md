# Users Spec Delta

## REMOVED Requirements

### Requirement: CQRS Handler Pattern for Users

**Reason**: CQRS pattern adds unnecessary complexity for user permission operations. Migrating to simpler service-based
architecture.

**Migration**: All handler logic migrated to `UserService`. Controllers updated to use services directly. No API
changes.

### Requirement: Command DTO for User Permissions

**Reason**: `UpdateUserPermissionsCommand` creates unnecessary transformation layer. Service can accept parameters
directly.

**Migration**: `UserService.updateUserPermissions()` accepts `UserId` and `Set<Authority>` directly. Command DTO
deleted.

### Requirement: Audit Logging for User Operations

**Reason**: Audit logging infrastructure (~350 lines) with only 6 usages does not justify complexity. No regulatory
requirement for audit trail in current scope.

**Migration**: `@Auditable` annotations removed from `UserService.updateUserPermissions()` and all other services. Audit
infrastructure deleted.

## MODIFIED Requirements

### Requirement: User Permission Management

The system SHALL provide user permission management through `UserService.updateUserPermissions()` which directly accepts
user ID and authority set without command DTO transformation. The system enforces business rule that at least one user
must have MEMBERS:PERMISSIONS authority to prevent admin lockout.

#### Scenario: Admin updates user permissions

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits PATCH /api/users/{id}/permissions
- **THEN** `UserService.updateUserPermissions()` validates requested authorities
- **AND** system checks that at least one user will have MEMBERS:PERMISSIONS after update
- **AND** user permissions are updated
- **AND** HTTP 200 OK is returned with updated user information

#### Scenario: Attempt to remove last permission manager

- **WHEN** update would remove MEMBERS:PERMISSIONS from last user with that authority
- **THEN** `UserService.updateUserPermissions()` rejects the update
- **AND** HTTP 400 Bad Request is returned
- **AND** response indicates cannot remove last permission manager

#### Scenario: Non-admin attempts permission change

- **WHEN** user without MEMBERS:PERMISSIONS authority attempts to change permissions
- **THEN** HTTP 403 Forbidden is returned
- **AND** response indicates insufficient permissions

#### Scenario: Invalid authority in request

- **WHEN** request contains invalid or non-existent authority
- **THEN** `UserService.updateUserPermissions()` validates before database operation
- **AND** HTTP 400 Bad Request is returned
- **AND** response lists invalid authorities

### Requirement: User Permission Query

The system SHALL provide user permission query through `UserService.getUserPermissions()` which returns the set of
authorities for a given user.

#### Scenario: Get user permissions

- **WHEN** authenticated user requests GET /api/users/{id}/permissions
- **THEN** `UserService.getUserPermissions()` returns user's authority set
- **AND** HTTP 200 OK is returned with permissions response
- **AND** response includes all granted authorities

#### Scenario: Query non-existent user

- **WHEN** requested user ID does not exist
- **THEN** HTTP 404 Not Found is returned
- **AND** response includes error details

### Requirement: Password Complexity Validation

Password complexity validation SHALL be implemented as a private method within `PasswordSetupService` instead of a
separate `PasswordComplexityValidator` component.

#### Scenario: Password fails complexity validation

- **WHEN** user submits password that doesn't meet complexity requirements
- **THEN** `PasswordSetupService.validatePassword()` (private method) throws `PasswordValidationException`
- **AND** exception message indicates all validation failures
- **AND** validation checks: minimum length, uppercase, lowercase, digit, special character
- **AND** validation checks password doesn't contain personal information (name, registration number)

#### Scenario: Password passes complexity validation

- **WHEN** user submits password meeting all requirements
- **THEN** `PasswordSetupService.validatePassword()` returns without exception
- **AND** password is accepted for setting
