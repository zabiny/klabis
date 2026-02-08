# Implementation Tasks: Remove ROLE field from User aggregate

## Overview

Remove the unused `roles` field from User aggregate, simplifying the domain model to use only authorities for
permissions.

## Tasks

### 1. Remove roles field from User domain aggregate ✓

**Description**: Remove `Set<UserRole> roles` field, `getRoles()` method, and roles parameter from all User factory
methods (create, createPendingActivation, reconstruct).

**Files to modify**:

- `klabis-backend/src/main/java/com/klabis/users/domain/User.java`
    - Remove line 52: `private Set<UserRole> roles = new HashSet<>();`
    - Remove lines 378-382: `getRoles()` method
    - Update factory methods to remove roles parameter:
        - `create(username, passwordHash, roles, authorities)` → `create(username, passwordHash, authorities)`
        - `create(registrationNumber, passwordHash, roles, authorities, accountStatus)` →
          `create(registrationNumber, passwordHash, authorities, accountStatus)`
        - `createPendingActivation(registrationNumber, passwordHash, roles, authorities)` →
          `createPendingActivation(registrationNumber, passwordHash, authorities)`
        - `reconstruct(id, username, passwordHash, roles, authorities, ...)` →
          `reconstruct(id, username, passwordHash, authorities, ...)`
    - Remove roles assignments in constructors and factory methods
    - Update toString() to remove roles reference

**Validation**:

- User.java compiles without errors
- UserTest.java fails (expected - will fix in later task)

---

### 2. Remove UserRole entity and Role enum ✓

**Description**: Delete UserRole.java entity and Role.java enum since they're no longer used.

**Files to delete**:

- `klabis-backend/src/main/java/com/klabis/users/domain/UserRole.java`
- `klabis-backend/src/main/java/com/klabis/users/domain/Role.java`

**Validation**:

- Files deleted
- Compilation errors expected (will fix in next tasks)

---

### 3. Update UserMemento to remove roles persistence ✓

**Description**: Remove roles field from UserMemento and update fromUser/toUser methods to stop handling roles.

**Files to modify**:

- `klabis-backend/src/main/java/com/klabis/users/infrastructure/jdbcrepository/UserMemento.java`
    - Remove line 47: `private Set<UserRole> roles;`
    - Remove lines 152-155: roles copying in `fromUser()`
    - Remove lines 202-204: roles conversion in `toUser()`
    - Remove line 293: `getRolesValue()` method

**Validation**:

- UserMemento.java compiles
- UserMementoTest.java fails (expected - will fix in later task)

---

### 4. Update UserCreatedEvent to remove roles field ✓

**Description**: Remove roles field from UserCreatedEvent record and update factory method.

**Files to modify**:

- `klabis-backend/src/main/java/com/klabis/users/domain/UserCreatedEvent.java`
    - Remove line 35: `private final Set<Role> roles;`
    - Update constructor to remove roles parameter
    - Update `fromUser()` method to stop extracting roles
    - Remove `getRoles()` getter method

**Validation**:

- UserCreatedEvent.java compiles
- Tests using the event fail (expected - will fix in later task)

---

### 5. Drop user_roles database table ✓

**Description**: Update database migration script to remove user_roles table creation and indexes.

**Files to modify**:

- `klabis-backend/src/main/resources/db/migration/V002__create_users_and_oauth2_tables.sql`
    - Remove lines 14-20: `CREATE TABLE user_roles` statement
    - Remove line 96: `CREATE INDEX idx_user_roles_user_id`
    - Remove line 110: `COMMENT ON TABLE user_roles`

**Validation**:

- Application starts successfully with H2 database
- Check logs: no errors about user_roles table
- Query H2 console: `SHOW TABLES` should not include user_roles

---

### 6. Update RegisterMemberCommandHandler ✓

**Description**: Remove Role.ROLE_MEMBER when creating users in member registration flow.

**Files to modify**:

- `klabis-backend/src/main/java/com/klabis/members/application/RegisterMemberCommandHandler.java`
    - Remove line 6: `import com.klabis.users.domain.Role;`
    - Update lines 125-130: Change
      `User.createPendingActivation(registrationNumber.getValue(), passwordHash, Set.of(Role.ROLE_MEMBER), Set.of("MEMBERS:READ"))`
      to `User.createPendingActivation(registrationNumber.getValue(), passwordHash, Set.of("MEMBERS:READ"))`

**Validation**:

- RegisterMemberCommandHandler.java compiles
- RegisterMemberAutoProvisioningTest.java passes

---

### 7. Update BootstrapDataLoader ✓

**Description**: Remove note about user_roles table no longer being used (lines 139-140).

**Files to modify**:

- `klabis-backend/src/main/java/com/klabis/config/BootstrapDataLoader.java`
    - Remove lines 139-140: Comment about user_roles table

**Validation**:

- BootstrapDataLoader.java compiles
- Application starts successfully
- Check logs: Admin user created without errors

---

### 8. Update test data builders and fixtures ✓

**Description**: Remove roles from UserTestData and UserTestDataBuilder.

**Files to modify**:

- `klabis-backend/src/test/java/com/klabis/users/testdata/UserTestData.java`
    - Remove all `Set.of(Role.ROLE_ADMIN)` and `Set.of(Role.ROLE_MEMBER)` from User.create() calls
    - Update lines 33, 45, 57, 69, 81, 93 to remove roles parameter
    - Remove line 111: `private Set<Role> roles = Set.of(Role.ROLE_MEMBER);`

- `klabis-backend/src/test/java/com/klabis/users/testdata/UserTestDataBuilder.java`
    - Remove line 40: `private Set<Role> roles = Set.of(Role.ROLE_MEMBER);`
    - Remove `.roles()` method calls in builder methods (lines 63, 84, 105)
    - Update User factory method calls to remove roles parameter

**Validation**:

- UserTestData.java compiles
- UserTestDataBuilder.java compiles

---

### 9. Update domain layer tests ✓

**Description**: Update all domain tests to remove Role usage.

**Files to modify**:

- `klabis-backend/src/test/java/com/klabis/users/domain/UserTest.java`
    - Update all `User.create()` calls to remove `Set.of(Role.ROLE_ADMIN)` or `Set.of(Role.ROLE_MEMBER)`
    - Remove any assertions on user.getRoles()

- `klabis-backend/src/test/java/com/klabis/users/domain/PasswordSetupTokenTest.java`
    - Update User.create() calls on lines 28, 51, 67, 82, 97, 120 to remove `Set.of(Role.ROLE_MEMBER)`

**Validation**:

- Run `mvn test -Dtest="UserTest"`
- Run `mvn test -Dtest="PasswordSetupTokenTest"`
- All tests pass

---

### 10. Update infrastructure layer tests ✓

**Description**: Update repository and infrastructure tests to remove roles.

**Files to modify**:

- `klabis-backend/src/test/java/com/klabis/users/infrastructure/jdbcrepository/UserJdbcRepositoryTest.java`
    - Update User factory calls to remove roles parameter

- `klabis-backend/src/test/java/com/klabis/users/infrastructure/jdbcrepository/UserMementoTest.java`
    - Update User factory calls to remove roles parameter
    - Remove any assertions on roles field

-
`klabis-backend/src/test/java/com/klabis/users/infrastructure/jdbcrepository/PasswordSetupTokenJdbcRepositoryTest.java`
    - Update User.create() calls to remove roles parameter

**Validation**:

- Run `mvn test -Dtest="*JdbcRepository*"`
- All repository tests pass

---

### 11. Update presentation layer tests ✓

**Description**: Update controller tests to remove roles.

**Files to modify**:

- `klabis-backend/src/test/java/com/klabis/users/presentation/UserControllerPermissionsTest.java`
    - Update User.create() calls on lines 137, 167, 196 to remove `Set.of(Role.ROLE_ADMIN)` or
      `Set.of(Role.ROLE_MEMBER)`

**Validation**:

- Run `mvn test -Dtest="UserControllerPermissionsTest"`
- All tests pass

---

### 12. Update application layer tests ✓

**Description**: Update application service and event listener tests.

**Files to modify**:

- `klabis-backend/src/test/java/com/klabis/users/application/PasswordSetupEventListenerTest.java`
    - Update User factory calls to remove roles parameter

- `klabis-backend/src/test/java/com/klabis/members/application/RegisterMemberAutoProvisioningTest.java`
    - Remove any Role-related assertions if present

**Validation**:

- Run `mvn test -Dtest="*EventListener*"`
- Run `mvn test -Dtest="RegisterMemberAutoProvisioningTest"`
- All tests pass

---

### 13. Run full test suite ✓

**Description**: Verify all tests pass after removing roles.

**Command**: `mvn test`

**Validation**:

- All unit tests pass
- All integration tests pass
- No compilation errors
- No runtime errors

---

### 14. Verify application starts and works correctly ✓

**Description**: Manual verification that application functions correctly without roles.

**Steps**:

1. Start application:
   `BOOTSTRAP_ADMIN_USERNAME='admin' BOOTSTRAP_ADMIN_PASSWORD='admin123' OAUTH2_CLIENT_SECRET='test-secret-123' mvn spring-boot:run`
2. Check logs for successful startup
3. Verify bootstrap admin user created
4. Check H2 console: `user_roles` table does not exist
5. Test OAuth2 authentication flow
6. Test member registration via API

**Validation**:

- Application starts without errors
- Admin user created successfully
- user_roles table does not exist
- Authentication works (uses authorities only)
- Member registration creates users successfully

---

## Task Dependencies

```
1 (User domain) → 3 (UserMemento) → 5 (DB table)
                → 4 (UserCreatedEvent)
                → 6 (RegisterMemberCommandHandler)
                → 7 (BootstrapDataLoader)

2 (Delete entities) (can be done after task 1)

8 (Test data) → 9 (Domain tests)
              → 10 (Infrastructure tests)
              → 11 (Presentation tests)
              → 12 (Application tests)
              → 13 (Full test suite)
              → 14 (Manual verification)
```

**Parallelizable**: Tasks 2, 3, 4, 6, 7 can be done in parallel after task 1 completes.

## Notes

- Application is not in production - no data migration needed
- Uses in-memory H2 database - table recreation on each startup
- Breaking change to UserCreatedEvent is acceptable (no production event consumers)
- All changes are mechanical refactoring - no business logic changes
