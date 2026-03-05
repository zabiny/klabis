# Design: Authentication and Authorization Separation

## Context

### Current State

The `users.authentication` package currently mixes authentication and authorization concerns:

```
com.klabis.users.authentication/
├── KlabisUserDetailsService          (authN - loads users for login)
├── PasswordSetupEventListener         (authN - password activation)
├── UserService                        (authZ - manages permissions)
└── UserController                     (authZ - permissions endpoints)
```

The `User` entity contains both identity data (password, account status) and authorities, creating tight coupling
between authentication and authorization.

### Problems

1. **Scalability constraint**: As authorization grows to include group-based delegation, context-specific authorities,
   and user-to-user grants, the mixed package becomes unwieldy
2. **Testing complexity**: Tests mix authentication mocking with authorization logic
3. **Violation of SRP**: Package has multiple reasons to change (authN bugs vs authZ features)
4. **Future group integration**: Groups will require contextual authorization (who, whose data, what authority) -
   difficult with current structure

### Stakeholders

- Backend developers: Need clear package boundaries and testable components
- Frontend developers: API must remain stable (no breaking changes to `/api/users/{id}/permissions`)
- Future groups feature: Will need clean integration point for contextual authorization

### Constraints

- Application in development with H2 in-memory database (no production data to migrate)
- Spring Security integration must continue working
- REST API contracts must remain unchanged
- Existing tests must be updated/refactored

## Goals / Non-Goals

**Goals:**

1. Separate authentication (who are you?) from authorization (what can you do?) at package and domain model level
2. Create `UserPermissions` aggregate separate from `User` entity
3. Introduce `AuthorizationContext` for contextual authorization checks
4. Design `AuthorizationQueryService` for extensible group-based authorization (future)
5. Distinguish global authorities (not group-grantable) from context-specific authorities
6. Move `PasswordSetupEventListener` to appropriate package (`passwordsetup`)
7. Maintain API compatibility and test coverage

**Non-Goals:**

1. Implementing group-based authorization (designing for it, not building it)
2. Changing REST API contracts
3. Implementing user-to-user permission grants (future feature)
4. Switching authentication technology (OAuth2 remains unchanged)
5. Changing how passwords are hashed/stored

## Decisions

### Decision 1: Create UserPermissions as Separate Aggregate

**Choice**: Create new `UserPermissions` aggregate separate from `User` entity

**Rationale**:

- Clean separation: User focuses on identity, UserPermissions on authorities
- Scalability: UserPermissions can grow complex without bloating User
- Independent evolution: Each aggregate can change independently
- Testability: Can test authorization without loading User credentials

**Alternatives considered**:

- *Option A*: Keep authorities in User, add roles field
    - Rejected: Doesn't separate concerns, User becomes more complex
- *Option B*: Split into separate bounded contexts (users vs accesscontrol)
    - Rejected: Over-engineering for current needs, can split later if justified
- *Option C*: Store authorities in both User and UserPermissions (sync)
    - Rejected: Data duplication, synchronization complexity, eventual inconsistency

**Implementation**:

```java
// UserPermissions aggregate
class UserPermissions {
    private final UserId userId;
    private Set<Authority> directAuthorities;

    void grantAuthority(Authority authority, AuthorizationPolicy policy) {
        policy.checkAdminLockoutPrevention(this.userId, authority);
        this.directAuthorities = new HashSet<>(this.directAuthorities);
        this.directAuthorities.add(authority);
    }
}

// User entity (simplified)
class User {
    private UserId id;
    private String username;
    private String passwordHash;
    private boolean enabled;
    // NO authorities field
}
```

### Decision 2: Package Structure Reorganization

**Choice**: Split `users.authentication` into three packages:

```
com.klabis.users/
├── authentication/                   # AuthN only
│   ├── KlabisUserDetailsService
│   └── (Spring Security adapters)
│
├── authorization/                    # AuthZ only
│   ├── UserPermissions                      (domain aggregate)
│   ├── AuthorizationContext                 (domain value object)
│   ├── AuthorizationPolicy                  (business rules)
│   ├── PermissionService                    (renamed from UserService)
│   ├── AuthorizationQueryService            (new)
│   ├── UserPermissionsRepository            (repository interface)
│   ├── PermissionController                 (renamed from UserController)
│   └── PermissionsResponseModelAssembler    (HAL+FORMS assembler)
│
└── passwordsetup/                     # User activation flow
    └── PasswordSetupEventListener        (moved from authentication)
```

**Rationale**:

- Clear boundary: Each package has single responsibility
- Easy navigation: Developers can find code by concern (authN vs authZ)
- Pragmatic structure: No over-engineering with excessive subpackages
- Future groups: `AuthorizationQueryService` has clear integration point

**Alternatives considered**:

- *Option A*: Keep UserService in authentication, add PermissionService
    - Rejected: UserService still handles permissions, confusion remains
- *Option B*: Create separate `permissions` module outside `users`
    - Rejected: Over-complicates, permissions are still about users
- *Option C*: DDD subpackages (domain/application/infrastructure/presentation)
    - Rejected: Over-engineering for current needs, simpler structure is clearer
- *Option D*: Keep everything in single authentication package
    - Rejected: Doesn't separate concerns, defeats purpose of refactoring

### Decision 3: AuthorizationContext for Contextual Authorization

**Choice**: Introduce `AuthorizationContext` value object:

```java
record AuthorizationContext(
    UserId actor,              // Who is making the request
    UserId resourceOwner,      // Whose data/resource is being accessed
    Authority requiredAuthority // What authority is needed
)
```

**Rationale**:

- Enables future group-based authorization: "Does actor have authority when accessing resourceOwner's data?"
- Explicit: Context is clear from method signature
- Type-safe: Cannot forget to pass required information
- Testable: Easy to create test contexts

**Alternatives considered**:

- *Option A*: Pass (actor, resourceOwner, authority) as separate parameters
    - Rejected: Error-prone, parameters can get reordered
- *Option B*: Use Spring Security's Authentication object only
    - Rejected: Doesn't capture resourceOwner, can't do contextual checks
- *Option C*: Check authorization at controller layer only
    - Rejected: Doesn't support service-layer authorization checks

### Decision 4: Authority Scope Classification

**Choice**: Add `Scope` enum to `Authority`:

```java
enum Authority {
    MEMBERS_PERMISSIONS("MEMBERS:PERMISSIONS", Scope.GLOBAL),
    SYSTEM_ADMIN("SYSTEM:ADMIN", Scope.GLOBAL),

    TRAINING_VIEW("TRAINING:VIEW", Scope.CONTEXT_SPECIFIC),
    TRAINING_MANAGE("TRAINING:MANAGE", Scope.CONTEXT_SPECIFIC),

    enum Scope {
        GLOBAL,              // Cannot be granted via groups
        CONTEXT_SPECIFIC     // Can be granted via groups
    }
}
```

**Rationale**:

- Prevents privilege escalation: Groups can't grant global admin
- Clear rules: Developers know which authorities can be group-grantable
- Future-proof: Easy to add new authorities with correct scope
- Validation: Can validate group authorization grants at aggregate level

**Alternatives considered**:

- *Option A*: Separate enums for global vs context-specific authorities
    - Rejected: Duplication, harder to use, need to check two enums
- *Option B*: String-based validation (check if authority starts with "SYSTEM:")
    - Rejected: Error-prone, no compile-time safety, magic strings
- *Option C*: All authorities group-grantable, use documentation
    - Rejected: Relies on developer discipline, no enforcement

### Decision 5: Database Schema

**Choice**: Create separate `user_permissions` table:

```sql
CREATE TABLE user_permissions (
    user_id      UUID PRIMARY KEY REFERENCES users(id),
    authorities  VARCHAR(1000) NOT NULL, -- JSON array stored as text
    created_at   TIMESTAMP,
    modified_at  TIMESTAMP
);
```

**Rationale**:

- Clear separation: User table for identity, user_permissions for authorities
- Performance: Can query permissions without loading User credentials
- Simple: JSON array stored as text (H2 supports JSON functions)
- Migration: Easy to create table, no existing data to migrate (dev environment)

**Alternatives considered**:

- *Option A*: One-to-many relationship (user_authorities table with rows per authority)
    - Rejected: Over-normalization, more joins, authorities are small set
- *Option B*: Store as comma-separated string
    - Rejected: Parsing complexity, no native query support
- *Option C*: Keep in users table
    - Rejected: Doesn't separate concerns, defeats purpose of refactoring

**Note**: Using JSON array stored as VARCHAR/TEXT is pragmatic for H2. If migrating to PostgreSQL later, can use native
JSONB type.

### Decision 6: AuthorizationQueryService Design

**Choice**: Create service with interface designed for future extension:

```java
interface AuthorizationQueryService {
    boolean checkAuthorization(AuthorizationContext context);
}

class AuthorizationQueryServiceImpl implements AuthorizationQueryService {
    private final UserPermissionsRepository permissionsRepo;
    // Future: private final MemberGroupRepository groupRepo;

    @Override
    public boolean checkAuthorization(AuthorizationContext context) {
        // Phase 1: Check direct authorities only
        UserPermissions permissions = permissionsRepo.findById(context.actor())
            .orElse(new UserPermissions(context.actor(), Set.of()));

        if (permissions.hasDirectAuthority(context.requiredAuthority())) {
            return true;
        }

        // Phase 2: Future - check group-based authorities
        // return checkGroupAuthorization(context);

        return false;
    }
}
```

**Rationale**:

- Interface-based: Easy to test, can mock
- Extensible: Clear place to add group logic later
- No breaking changes: Adding group checks doesn't change interface
- Pragmatic: Implements current requirements, designs for future

**Alternatives considered**:

- *Option A*: Use Spring Security's `hasAuthority()` in annotations only
    - Rejected: Doesn't support contextual authorization (whose data?)
- *Option B*: Create separate service for group authorization
    - Rejected: Two services means two places to check, complexity
- *Option C*: Put authorization logic in controllers
    - Rejected: Duplicates logic, hard to test, can't reuse in services

### Decision 7: Repository Interface Design

**Choice**: Create `UserPermissionsRepository` interface:

```java
interface UserPermissionsRepository {
    UserPermissions save(UserPermissions permissions);
    Optional<UserPermissions> findById(UserId userId);
    long countUsersWithAuthority(Authority authority); // For admin lockout
}
```

**Rationale**:

- Follows existing pattern: Matches `UserRepository` style
- Domain-driven: Repository interface in domain, impl in infrastructure
- Testable: Easy to mock in unit tests

### Decision 8: Service Renaming

**Choice**: Rename services and controllers for clarity:

- `UserService` → `PermissionService`
- `UserController` → `PermissionController`

**Rationale**:

- Clarity: Name indicates what service manages (permissions, not users)
- Consistency: Aligns with new package structure (`authorization`)
- Avoids confusion: No collision with potential future `user-management` service

**Alternatives considered**:

- *Option A*: Keep names, only move packages
    - Rejected: `UserService` in `authorization` package is confusing
- *Option B*: Rename to `UserAuthorizationService`
    - Rejected: Too verbose, `PermissionService` is clearer

## Risks / Trade-offs

### Risk 1: Breaking Spring Security Integration

**Risk**: Moving authorities from User to UserPermissions could break OAuth2 token generation and Spring Security's
`UserDetailsService`.

**Mitigation**:

- Update `KlabisUserDetailsService` to load both User and UserPermissions
- Ensure authorities are still included in JWT access token claims
- Add integration tests for OAuth2 flow after refactoring
- Test token generation and validation explicitly

**Validation**:

```java
// KlabisUserDetailsService must load both
UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username).orElseThrow();
    UserPermissions permissions = permissionsRepo.findById(user.getId())
        .orElse(new UserPermissions(user.getId(), Set.of()));
    return new KlabisUserDetails(user, permissions);
}
```

### Risk 2: Authorization Checks Missed in Refactoring

**Risk**: Existing authorization checks throughout codebase might not be updated to use `AuthorizationQueryService`.

**Mitigation**:

- Search codebase for all `hasAuthority()` calls
- Search for all `@PreAuthorize` annotations
- Create integration test covering all secured endpoints
- Use compiler to find references to old `User.getAuthorities()`

**Validation**:

```bash
# Find all authorization checks
grep -r "hasAuthority\|@PreAuthorize\|getAuthorities()" src/
```

### Risk 3: Database Migration Complexity

**Risk**: While H2 in-memory means no production data, the table creation and data seeding must work correctly.

**Mitigation**:

- Create Flyway migration to create `user_permissions` table
- Update `BootstrapDataLoader` to create both User and UserPermissions
- Test bootstrap process creates admin user with permissions
- Verify `admin` user can authenticate and has permissions

**Validation**:

```sql
-- Flyway migration
CREATE TABLE user_permissions (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    authorities VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Risk 4: Test Refactoring Errors

**Risk**: Renaming classes and moving packages could break tests, especially tests using reflection or package-private
access.

**Mitigation**:

- Run full test suite after each package move
- Update test imports and class names
- Verify all tests pass before committing
- Check for tests using `.class` or package names as strings

### Risk 5: Over-Engineering for Future Groups

**Risk**: Designing for group-based authorization might add unnecessary complexity if groups feature changes or is
cancelled.

**Mitigation**:

- Design for extensibility but don't implement groups
- Keep `AuthorizationQueryService` simple (only direct authorities initially)
- Document where group integration would go
- YAGNI principle: Don't build groups until needed

**Trade-off**: Additional abstraction layer (AuthorizationContext) vs clean extensibility. Acceptable given explicit
requirements for future groups.

## Migration Plan

### Phase 1: Prepare (Week 1)

1. Create `UserPermissions` aggregate class (in new package location later)
2. Create `AuthorizationContext` and `AuthorizationPolicy` classes
3. Create `UserPermissionsRepository` interface and JPA implementation
4. Write unit tests for new classes

### Phase 2: Database Schema (Week 1)

1. Create Flyway migration VXXX__create_user_permissions_table.sql
2. Update `BootstrapDataLoader` to create `UserPermissions` for admin user
3. Test bootstrap creates admin with `MEMBERS:PERMISSIONS`
4. Verify authentication still works (admin can login)

### Phase 3: Create Authorization Package (Week 1-2)

1. Create `com.klabis.users.authorization` package (flat structure)
2. Create domain classes: `UserPermissions`, `AuthorizationContext`, `AuthorizationPolicy`
3. Create repository interface: `UserPermissionsRepository` and JPA implementation
4. Move `UserService` → `PermissionService` (update class, rename file, update tests)
5. Move `UserController` → `PermissionController` (update class, rename file, update tests)
6. Create `AuthorizationQueryService` implementation
7. Update repository references throughout codebase

### Phase 4: Refactor User Entity (Week 2)

1. Remove `authorities` field from `User` entity
2. Update `User` repository queries to not use authorities
3. Update `KlabisUserDetailsService` to load `UserPermissions`
4. Verify OAuth2 token generation includes authorities
5. Test authentication flow end-to-end

### Phase 5: Move PasswordSetupEventListener (Week 2)

1. Move `PasswordSetupEventListener` to `users.passwordsetup` package
2. Update tests (package imports)
3. Verify password setup flow still works

### Phase 6: Update Authorization Checks (Week 2-3)

1. Find all authorization checks in codebase
2. Replace `User.getAuthorities()` with `AuthorizationQueryService`
3. Update `@PreAuthorize` annotations if needed
4. Add integration tests for contextual authorization
5. Verify all secured endpoints still work

### Phase 7: Cleanup and Verification (Week 3)

1. Delete old `users.authentication` package (now empty after moving classes)
2. Run full test suite and verify all tests pass
3. Manual testing: authentication, authorization, permissions management
4. Update documentation (ARCHITECTURE.md, package-info.java files)
5. Code review and final adjustments

### Rollback Strategy

Since application uses H2 in-memory database with no persistent data:

- **Rollback is simple**: Revert code changes, restart application
- **Data loss**: None (data recreated on next restart)
- **Risk**: Low (development environment)

If rollback needed:

1. `git revert` commits for this change
2. Restart application
3. Admin user recreated with original structure

## Open Questions

### Q1: Should UserPermissions be created lazily or eagerly?

**Decision**: **Lazy creation**

**Rationale**:

- UserPermissions is only needed if user has direct authorities
- Bootstrap data creates admin with permissions immediately anyway
- Most users will have at least one authority (especially via groups in future)
- Avoids creating empty records unnecessarily

**Implementation**:

- `UserPermissionsRepository.findById()` returns `Optional.empty()` if not exists
- `AuthorizationQueryService` treats missing UserPermissions as empty authorities
- `PermissionService.grantAuthority()` creates UserPermissions if it doesn't exist

### Q2: How should JSON authorities be serialized in database?

**Decision**: **JSON array stored as VARCHAR(1000)**

**Format example**: `["MEMBERS:READ","TRAINING:VIEW"]`

**Rationale**:

- JSON array is standard, readable, and well-supported
- H2 supports JSON functions for querying if needed
- VARCHAR(1000) sufficient (most users have <10 authorities)
- Simple to serialize/deserialize with Jackson
- Easy to migrate to PostgreSQL JSONB later if needed

**Implementation**:

```java
// Repository stores/loads JSON string
class UserPermissions {
    private Set<Authority> directAuthorities;

    String getAuthoritiesJson() {
        return new ObjectMapper().writeValueAsString(
            directAuthorities.stream()
                .map(Authority::getValue)
                .collect(Collectors.toList())
        );
    }

    void setAuthoritiesJson(String json) {
        List<String> values = new ObjectMapper().readValue(json,
            new TypeReference<List<String>>() {});
        this.directAuthorities = values.stream()
            .map(Authority::fromValue)
            .collect(Collectors.toSet());
    }
}
```

### ~~Q3: Should AuthorizationQueryService be a Spring Bean or utility class?~~

**Decision**: **@Service bean** (resolved in Decision 6)

**Implementation**:

```java
@Service
public class AuthorizationQueryService {
    private final UserPermissionsRepository permissionsRepo;

    public AuthorizationQueryService(UserPermissionsRepository permissionsRepo) {
        this.permissionsRepo = permissionsRepo;
    }

    public boolean checkAuthorization(AuthorizationContext context) {
        // Check direct authorities
        // Future: Check group-based authorities
    }
}
```

### ~~Q4: Should we have DDD-style subpackages?~~

**Decision**: **Flat package structure** (resolved in Decision 2)

**Implementation**: All classes in `com.klabis.users.authorization`:

- Domain: `UserPermissions`, `AuthorizationContext`, `AuthorizationPolicy`
- Services: `PermissionService`, `AuthorizationQueryService`
- Repository: `UserPermissionsRepository`
- Controller: `PermissionController`
- Assembler: `PermissionsResponseModelAssembler`

### Q5: How to handle missing UserPermissions in queries?

**Decision**: **Treat as empty authorities (return false)**

**Rationale**:

- Missing UserPermissions means no direct authorities assigned
- Authorization check returns `false` (not authorized)
- UserPermissions record created lazily when first authority is granted via `PermissionService`
- Avoids creating empty records for users without permissions

**Implementation**:

```java
@Service
public class AuthorizationQueryService {
    public boolean checkAuthorization(AuthorizationContext context) {
        // Return empty UserPermissions if not found (treat as no authorities)
        UserPermissions permissions = permissionsRepo.findById(context.actor())
            .orElse(new UserPermissions(context.actor(), Set.of()));

        return permissions.hasDirectAuthority(context.requiredAuthority());
    }
}
```
