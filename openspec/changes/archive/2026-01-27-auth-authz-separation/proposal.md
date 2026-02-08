# Proposal: Authentication and Authorization Separation

## Why

The current `users.authentication` package mixes two orthogonal concerns: **authentication** (verifying user identity)
and **authorization** (managing permissions). This coupling creates technical debt that will hinder future scalability
as the authorization model grows to include:

- Context-specific authorities for each module (training, competitions, membership, etc.)
- Group-based delegated authorization (group admins receive group authorities when working with group members' data)
- User-to-user permission grants (future feature)

The package structure already shows this confusion - `UserService` and `UserController` handle permission management (
authorization), while `KlabisUserDetailsService` handles authentication. However, `PasswordSetupEventListener` is
misplaced - it handles password setup/activation flow and belongs in the `passwordsetup` package, not authentication.
Separating these concerns now creates clean boundaries for future growth and improves maintainability, testability, and
clarity.

## What Changes

- **Extract UserPermissions aggregate** - Create new domain aggregate separate from User entity to handle direct
  authorities only
- **Separate packages** - Split `users.authentication` into `users.authentication` (authN only) and
  `users.authorization` (authZ only)
- **Move PasswordSetupEventListener** - Relocate from `authentication` to `passwordsetup` package where it belongs
- **Introduce AuthorizationContext** - New domain concept to support contextual authorization checks (actor,
  resourceOwner, requiredAuthority)
- **Add AuthorizationQueryService** - Service to check authorization considering both direct authorities and group-based
  delegation
- **Distinguish authority scopes** - Separate global authorities (not group-grantable) from context-specific
  authorities (group-grantable)
- **Remove authorities from User entity** - User entity becomes identity-only (credentials, account status)
- **Update controller naming** - Rename `UserController` to `PermissionController` for clarity
- **Maintain API compatibility** - Existing REST endpoints remain unchanged during migration

## Capabilities

### New Capabilities

- `user-permissions`: User permissions management with contextual authorization support
    - Direct authority assignment and revocation
    - Authorization checks based on context (who, whose data, what authority)
    - Integration with member groups for delegated authorization
    - Support for both global and context-specific authorities

### Modified Capabilities

- `users`: User entity refactored to identity-only (removes authorities from User aggregate)
    - User entity no longer contains authorities
    - Authorities managed in separate UserPermissions aggregate
    - Authentication flow unchanged (login, password setup still work)
    - Authorization checks now query UserPermissions instead of User

## Impact

**Affected Code:**

- `com.klabis.users.authentication` package - split into two packages
- `com.klabis.users.User` entity - removes authorities field
- `com.klabis.users.authentication.PasswordSetupEventListener` - moves to `users.passwordsetup` package
- `com.klabis.users.authentication.UserService` - moves to `users.authorization` as `PermissionService`
- `com.klabis.users.authentication.UserController` - moves to `users.authorization` as `PermissionController`
- All authorization checks throughout codebase - update to use `AuthorizationQueryService`

**Affected APIs:**

- No breaking changes to REST API endpoints
- `/api/users/{id}/permissions` - unchanged externally
- Internal authorization check mechanism changes (implementation detail)

**Dependencies:**

- No new external dependencies
- `AuthorizationQueryService` designed for future extensibility to integrate with member groups (implementation detail
  to be decided when groups feature is developed)
- Spring Security integration unchanged

**Migration:**

- No database migration needed (H2 in-memory, development environment)
- New user_permissions table created via Flyway migration
- Existing data automatically reloaded on next application restart

**Testing:**

- All existing tests for UserService/UserController to be updated/renamed
- New tests for UserPermissions aggregate and AuthorizationQueryService
- Authorization integration tests to cover both direct and group-based scenarios
