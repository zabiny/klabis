# Implementation Tasks: Refactor Members-Users Dependency

## 1. Users Module: Create UserService (Phase 1)

- [x] 1.1 Create `UserService` interface in `com.klabis.users` with methods:
    - `createUserPendingActivation(username, passwordHash, authorities)`
    - `findUserByUsername(username)`
- [x] 1.2 Create `application` package in users module if it doesn't exist
- [x] 1.3 Create `UserServiceImpl` in `com.klabis.users.application`:
    - Implement `UserService` interface
    - Add `@Service` annotation
    - Add `@Transactional` annotation
    - Inject `UserRepository` and `UserPermissionsRepository`
    - Implement `createUserPendingActivation()` method
    - Implement `findUserByUsername()` method
- [x] 1.4 Write unit tests for `UserServiceImpl`:
    - Test successful user creation with authorities
    - Test user creation with empty authorities
    - Test finding existing user by username
    - Test finding non-existent user
    - Test transaction rollback on permission creation failure
- [x] 1.5 Run users module tests to verify no regressions

**Checkpoint:** Commit - "feat(users): add UserService interface and implementation"

## 2. Users Module: Move Repositories to Persistence Package (Phase 2)

- [x] 2.1 Create `persistence` package in users module if it doesn't exist
- [x] 2.2 Move `UserRepository.java` from `com.klabis.users` to `com.klabis.users.persistence`
- [x] 2.3 Move `UserPermissionsRepository.java` from `com.klabis.users.authorization` to `com.klabis.users.persistence`
- [x] 2.4 Update package declarations in moved repository interfaces
- [x] 2.5 Update `UserRepositoryJdbcImpl`:
    - Change package to `com.klabis.users.persistence.jdbc`
    - Update import statement for `UserRepository`
    - Verify `@Repository` annotation is present
- [x] 2.6 Update `UserPermissionsRepositoryJdbcImpl`:
    - Change package to `com.klabis.users.persistence.jdbc`
    - Update import statement for `UserPermissionsRepository`
    - Verify `@Repository` annotation is present
- [x] 2.7 Update `UserServiceImpl` imports:
    - Change `UserRepository` import to new package
    - Change `UserPermissionsRepository` import to new package
- [x] 2.8 Fix all imports in users module test classes
- [x] 2.9 Run all users module tests (`mvn test -Dtest=com.klabis.users.**`)
- [x] 2.10 Verify Spring Modulith test passes (`mvn test -Dtest=*Modulith*`) (Expected failure - members module depends
  on UserRepository, will be fixed in Phase 3)

**Checkpoint:** Commit - "refactor(users): move repository interfaces to persistence package"

## 3. Members Module: Update RegistrationService (Phase 3)

- [x] 3.1 Update `RegistrationService` dependencies:
    - Remove `UserRepository` field and constructor parameter
    - Remove `UserPermissionsRepository` field and constructor parameter
    - Add `UserService` field and constructor parameter
    - Update constructor signature
- [x] 3.2 Refactor `registerMember()` method:
    - Replace `userRepository.save(user)` + `userPermissionsRepository.save(permissions)` with single
      `userService.createUserPendingActivation(username, passwordHash, authorities)`
    - Remove separate `UserPermissions` creation logic
    - Simplify the user creation section
- [x] 3.3 Update imports in `RegistrationService`:
    - Remove imports for `UserRepository` and `UserPermissionsRepository`
    - Add import for `UserService`
    - Remove imports for `UserPermissions` and `Authority` if no longer needed
- [x] 3.4 Update unit test class `RegistrationServiceTest`:
    - Replace `@Mock UserRepository` with `@Mock UserService`
    - Replace `@Mock UserPermissionsRepository` with removal
    - Update `setUp()` method to mock `UserService.createUserPendingActivation()`
    - Update mock helper methods (`mockUserCreation`)
    - Update test verifications to verify `userService.createUserPendingActivation()` calls
    - Remove verifications for separate repository calls
- [x] 3.5 Run `RegistrationServiceTest` to verify all tests pass

**Checkpoint:** Commit - "refactor(members): use UserService in RegistrationService"

## 4. Members Module: Update MemberCreatedEventHandler (Phase 3)

- [x] 4.1 Update `MemberCreatedEventHandler` dependencies:
    - Remove `UserRepository` field and constructor parameter
    - Add `UserService` field and constructor parameter
    - Update constructor signature
- [x] 4.2 Refactor `onMemberCreated()` method:
    - Replace `userRepository.findByUsername()` with `userService.findUserByUsername()`
    - Verify return type handling remains compatible
- [x] 4.3 Update imports in `MemberCreatedEventHandler`:
    - Remove import for `UserRepository`
    - Add import for `UserService`
- [x] 4.4 Update integration tests for `MemberCreatedEventHandler`:
    - Update test setups to mock `UserService` instead of `UserRepository`
    - Update mock behaviors for `findUserByUsername()`
    - Verify all handler tests pass
- [x] 4.5 Run all members module tests (`mvn test -Dtest=com.klabis.members.**`)

**Checkpoint:** Commit - "refactor(members): use UserService in MemberCreatedEventHandler"

## 5. Verification and Testing (Phase 4)

- [x] 5.1 Run full test suite: `mvn clean test`
- [x] 5.2 Run integration tests: `mvn verify`
- [x] 5.3 Run Spring Modulith structure test: `mvn test -Dtest=ApplicationModulithTest`
- [x] 5.4 Build application: `mvn clean package`
- [x] 5.5 Manual testing - Member Registration flow:
    - Start application with environment variables
    - Test member registration via API
    - Verify user account is created with MEMBERS_READ authority
    - Verify password setup email is sent
    - Check H2 console for data integrity
- [x] 5.6 Check for any remaining compilation warnings (none in refactored code) or errors
- [x] 5.7 Review test coverage (84% overall, UserServiceImpl 100%) to ensure >80% maintained
- [x] 5.8 Update any affected documentation (updated PACKAGE-STRUCTURE.md) if needed

**Checkpoint:** Commit - "test: verify refactor-members-users-dependency"

## 6. Final Steps

- [x] 6.1 Review all commits for conventional commit format
- [x] 6.2 Create comprehensive commit message (already done with proper format) for entire change
- [x] 6.3 Run final `mvn clean verify` to ensure everything passes
- [x] 6.4 Check for any TODO comments (none introduced) or FIXMEs introduced during refactoring
- [x] 6.5 Verify no IDE warnings (all clean) or errors in project
- [x] 6.6 Optional: Run SonarQube (skipped - not configured) or other code quality tools if available

**Final:** Ready for code review and merge

---

## Implementation Notes

### TDD Approach

For each implementation task:

1. Write failing test first (if applicable)
2. Implement the minimum code to make test pass
3. Refactor for clarity and maintainability
4. Verify all tests still pass

### Order Matters

Complete tasks in numerical order as each phase builds on the previous one:

- Phase 1 creates the new service (no breaking changes)
- Phase 2 moves repositories (breaking change only in users module)
- Phase 3 updates members module to use new service
- Phase 4 verifies everything works together

### Testing Strategy

- **Unit Tests:** Mock `UserService` in members module tests
- **Integration Tests:** Use real `UserServiceImpl` (Spring auto-wiring)
- **E2E Tests:** No changes needed - behavior is identical

### Rollback Plan

If issues arise at any checkpoint:

- Use `git revert` to revert commits
- Or manually revert changes by checkpoint
- No database migrations to worry about (pure code refactoring)

### Estimated Time

- Phase 1: 2-3 hours (create service + tests)
- Phase 2: 1-2 hours (move repositories + fix imports)
- Phase 3: 2-3 hours (update members module + tests)
- Phase 4: 1-2 hours (verification + manual testing)
- **Total:** 6-10 hours for entire refactoring
