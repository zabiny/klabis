# Proposal: Refactor Members-Users Dependency

## Why

The members module currently depends on repository interfaces (`UserRepository`, `UserPermissionsRepository`) from the
users module, violating Clean Architecture principles. Repository interfaces are persistence abstractions that should
not be exposed across module boundaries. This creates tight coupling and leaks implementation details between bounded
contexts.

**Current State:**

```
members → users.UserRepository (persistence abstraction) ❌
members → users.UserPermissionsRepository (persistence abstraction) ❌
```

**Desired State:**

```
members → users.UserService (business operation) ✓
```

## What Changes

### Users Module Refactoring

- **Move** `UserRepository` interface from `com.klabis.users` to `com.klabis.users.persistence`
- **Move** `UserPermissionsRepository` interface from `com.klabis.users.authorization` to `com.klabis.users.persistence`
- **Create** `UserService` interface in `com.klabis.users` (module root) with two methods:
    - `UserId createUserPendingActivation(String username, String passwordHash, Set<Authority> authorities)`
    - `Optional<User> findUserByUsername(String username)`
- **Create** `UserServiceImpl` in `com.klabis.users.application` implementing `UserService`
    - Internally uses `UserRepository` and `UserPermissionsRepository`
    - Handles user creation with permissions in a single transaction

### Members Module Refactoring

- **Update** `RegistrationService` to depend on `UserService` instead of repositories
    - Replace `userRepository.save()` + `userPermissionsRepository.save()` with single
      `userService.createUserPendingActivation()`
    - Remove direct dependency on `UserRepository` and `UserPermissionsRepository`
- **Update** `MemberCreatedEventHandler` to depend on `UserService` instead of `UserRepository`
    - Replace `userRepository.findByUsername()` with `userService.findUserByUsername()`

### Package Structure Changes

**Before:**

```
com.klabis.users/
  ├── UserRepository.java (root - exposed)
  ├── User.java
  └── authorization/
      └── UserPermissionsRepository.java (exposed)
```

**After:**

```
com.klabis.users/
  ├── UserService.java (root - business interface)
  ├── User.java
  ├── application/
  │   └── UserServiceImpl.java
  └── persistence/
      ├── UserRepository.java (hidden)
      ├── UserPermissionsRepository.java (hidden)
      └── jdbc/
          ├── UserJdbcRepository.java
          └── UserPermissionsJdbcRepository.java
```

### Transaction Boundary

- `UserServiceImpl.createUserPendingActivation()` will handle the transaction for creating both User and UserPermissions
- This simplifies `RegistrationService` - no longer needs to coordinate multi-step user creation

## Capabilities

### New Capabilities

None - this is an internal architectural refactoring with no new user-facing capabilities.

### Modified Capabilities

None - no changes to external API contracts or business requirements.

## Impact

### Affected Components

- **Users Module:**
    - `UserRepository` - move to persistence package
    - `UserPermissionsRepository` - move to persistence package
    - `UserJdbcRepository` - update package declaration
    - `UserPermissionsJdbcRepository` - update package declaration
    - `UserService` (new) - business service interface
    - `UserServiceImpl` (new) - service implementation

- **Members Module:**
    - `RegistrationService` - replace repository dependencies with `UserService`
    - `MemberCreatedEventHandler` - replace `UserRepository` with `UserService`

- **Tests:**
    - Update imports in all affected test classes
    - Mock `UserService` instead of repositories in members module tests

### Architecture Benefits

- ✅ Proper separation of concerns - business operations vs. persistence
- ✅ Members module depends on business abstractions, not infrastructure
- ✅ Easier to test - mock one service instead of two repositories
- ✅ Better encapsulation - repository implementations hidden in persistence package
- ✅ Clearer module boundaries - follows Spring Modulith best practices

### Migration Risk

- **Low Risk:** Pure refactoring, no behavior changes
- All changes are internal to the codebase
- No API changes, no database schema changes
- Existing tests should pass after updating imports

## Dependencies

None - this is a self-contained refactoring within the existing codebase.
