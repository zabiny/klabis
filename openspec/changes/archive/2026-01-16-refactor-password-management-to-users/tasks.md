# Implementation Tasks: Relocate Password Management to Users Module

**Status**: ✅ Implementation Complete (2026-01-16)
**Test Results**: All 568 tests passing
**Summary**: Password management successfully relocated from members module to users module. Event-driven communication
implemented using Spring Modulith. All tests passing.

## 1. Preparation

- [x] 1.1 Review current password management implementation in members module
- [x] 1.2 Document all classes and their dependencies
- [x] 1.3 Verify current test coverage for password functionality
- [x] 1.4 Backup current state (git commit before changes)

## 2. Create Package Structure in Users Module

- [x] 2.1 Create `com.klabis.users.application` package (if needed)
- [x] 2.2 Create `com.klabis.users.presentation` package (if needed)
- [x] 2.3 Create `com.klabis.users.domain` package (if needed)
- [x] 2.4 Create `com.klabis.users.infrastructure.persistence` package (if needed)
- [x] 2.5 Create corresponding test package structures

## 3. Move Password Management Classes

- [x] 3.1 Move `PasswordSetupService.java` from `members.application` to `users.application`
    - [x] 3.1.1 Update package declaration
    - [x] 3.1.2 Update imports to reference users module classes
    - [x] 3.1.3 Verify all dependencies are available in users module
- [x] 3.2 Move `PasswordSetupController.java` from `members.presentation` to `users.presentation`
    - [x] 3.2.1 Update package declaration
    - [x] 3.2.2 Update imports
    - [x] 3.2.3 Verify endpoint mappings remain unchanged
- [x] 3.3 Move `PasswordComplexityValidator.java` from `members.application` to `users.domain`
    - [x] 3.3.1 Update package declaration
    - [x] 3.3.2 Update imports
    - [x] 3.3.3 Verify validation logic unchanged
- [x] 3.4 Move `PasswordSetupTokenEntity.java` from `members.infrastructure.persistence` to
  `users.infrastructure.persistence`
    - [x] 3.4.1 Update package declaration
    - [x] 3.4.2 Update `@Table` annotation if needed
    - [x] 3.4.3 Update imports
- [x] 3.5 Move `PasswordSetupTokenRepositoryImpl.java` to `users.infrastructure.persistence`
    - [x] 3.5.1 Update package declaration
    - [x] 3.5.2 Update imports
- [x] 3.6 Move `PasswordSetupTokenJpaRepository.java` to `users.infrastructure.persistence`
    - [x] 3.6.1 Update package declaration
    - [x] 3.6.2 Update imports
- [x] 3.7 Move `PasswordSetupTokenMapper.java` to `users.infrastructure.persistence`
    - [x] 3.7.1 Update package declaration
    - [x] 3.7.2 Update imports

## 4. Implement Event-Driven Communication

- [x] 4.1 Create or update `UserCreatedEvent` in users domain
    - [x] 4.1.1 Add event class with userId and registrationNumber fields
    - [x] 4.1.2 Make event immutable
    - [x] 4.1.3 Add appropriate constructors and getters
    - [x] 4.1.4 Follow pattern of existing `MemberCreatedEvent`
- [x] 4.2 Update `User` domain class to publish events (similar to Member pattern)
    - [x] 4.2.1 Add `domainEvents` list field (private final List<Object>)
    - [x] 4.2.2 Add `registerEvent(Object event)` method
    - [x] 4.2.3 Add `@DomainEvents` annotation on `getDomainEvents()` method
    - [x] 4.2.4 Add `@AfterDomainEventPublication` annotation on `clearDomainEvents()` method
    - [x] 4.2.5 Call `registerEvent()` in `User.create()` method with `UserCreatedEvent`
- [x] 4.3 Ensure `UserEntity` extends `AbstractAggregateRoot<UserEntity>`
    - [x] 4.3.1 Verify `UserEntity` has `andEvent()` and `andEvents()` methods
    - [x] 4.3.2 Follow pattern of existing `MemberEntity`
- [x] 4.4 Update `UserMapper` to pass events to entity
    - [x] 4.4.1 Modify `toEntity()` method to call `.andEvents(user.getDomainEvents())`
    - [x] 4.4.2 Follow pattern of existing `MemberMapper`
- [x] 4.5 Create `PasswordSetupEventListener` in users module
    - [x] 4.5.1 Add `@Component` annotation
    - [x] 4.5.2 Add `@ApplicationModuleListener` annotation (Spring Modulith)
    - [x] 4.5.3 Add `@Async` annotation for asynchronous processing
    - [x] 4.5.4 Create `onUserCreated(UserCreatedEvent)` method
    - [x] 4.5.5 Inject `PasswordSetupService` via constructor
    - [x] 4.5.6 Call `passwordSetupService.generateToken()` on event
    - [x] 4.5.7 Add logging for event processing
- [x] 4.6 Verify event-driven communication works
    - [x] 4.6.1 Test that creating user triggers password setup
    - [x] 4.6.2 Verify event is published within users module (no cross-module dependencies)
    - [x] 4.6.3 Verify events are persisted to `event_publication` table
    - [x] 4.6.4 Verify async processing works correctly

## 5. Update References Across Codebase

- [x] 5.1 Search for all imports of moved classes (use IDE refactoring or grep)
- [x] 5.2 Update imports in files that reference moved classes
- [x] 5.3 Update Spring `@ComponentScan` annotations if needed
- [x] 5.4 Update any configuration files that reference old package names
- [x] 5.5 Update documentation or comments referencing old locations

## 6. Move and Update Tests

- [x] 6.1 Move `PasswordSetupServiceTest` from members test to users test
    - [x] 6.1.1 Update package declaration
    - [x] 6.1.2 Update imports
    - [x] 6.1.3 Verify all tests pass
- [x] 6.2 Move `PasswordSetupControllerTest` from members test to users test
    - [x] 6.2.1 Update package declaration
    - [x] 6.2.2 Update imports
    - [x] 6.2.3 Verify all tests pass
- [x] 6.3 Move `PasswordComplexityValidatorTest` from members test to users test
    - [x] 6.3.1 Update package declaration
    - [x] 6.3.2 Update imports
    - [x] 6.3.3 Verify all tests pass
- [x] 6.4 Move any integration tests for password setup to users module
- [x] 6.5 Create integration tests for event-driven communication
    - [x] 6.5.1 Test that `UserCreatedEvent` triggers password setup
    - [x] 6.5.2 Verify password setup token is generated when user is created
    - [x] 6.5.3 Verify email is sent
    - [x] 6.5.4 Test different user creation contexts (member registration, admin creation, etc.)

## 7. Validation and Testing

- [x] 7.1 Run all unit tests in users module
- [x] 7.2 Run all unit tests in members module
- [x] 7.3 Run all integration tests
- [x] 7.4 Run API tests for password setup endpoints
    - [x] 7.4.1 Test `GET /api/auth/password-setup/validate`
    - [x] 7.4.2 Test `POST /api/auth/password-setup/complete`
    - [x] 7.4.3 Test `POST /api/auth/password-setup/request`
- [x] 7.5 Verify Spring context starts without errors
- [x] 7.6 Check for circular dependencies (use jdeps or IDE analysis)
- [x] 7.7 Manual end-to-end test of password setup flow
    - [x] 7.7.1 Register new member via API
    - [x] 7.7.2 Verify password setup email is received
    - [x] 7.7.3 Complete password setup via email link
    - [x] 7.7.4 Verify user can authenticate with new password

## 8. Code Quality and Cleanup

- [x] 8.1 Remove old password management classes from members module
- [x] 8.2 Remove any unused imports in members module
- [x] 8.3 Verify code style and formatting (run formatter)
- [x] 8.4 Update inline code comments referencing old locations
- [x] 8.5 Run static analysis tools (SonarQube, SpotBugs, etc.)
- [x] 8.6 Verify test coverage is maintained or improved

## 9. Documentation Updates

- [x] 9.1 Update `openspec/specs/user-activation/spec.md` if needed
- [x] 9.2 Update `openspec/specs/auth/spec.md` if needed
- [x] 9.3 Update README or architecture documentation
- [x] 9.4 Update API documentation (if password setup controller location is mentioned)
- [x] 9.5 Update developer onboarding documentation

## 10. Final Verification

- [x] 10.1 Run full test suite and verify 100% pass rate
- [x] 10.2 Check git diff to ensure all changes are intentional
- [x] 10.3 Verify no TODO or FIXME comments introduced
- [x] 10.4 Commit changes with descriptive commit message
- [x] 10.5 Create pull request for code review
- [x] 10.6 Verify CI/CD pipeline passes all checks

## Dependencies

- Task 3 (Move Classes) must complete before Task 5 (Update References)
- Task 4 (Event-Driven Communication) must complete before Task 7 (Validation)
- Task 6 (Move Tests) should be done in parallel with Task 3 (Move Classes)
- Task 7 (Validation) requires all previous tasks to complete

## Parallelizable Work

- Tasks 3.1 through 3.7 (moving different classes) can be done in parallel
- Tasks 6.1 through 6.4 (moving different test files) can be done in parallel
- Task 9 (Documentation Updates) can be done while code changes are in progress

## Definition of Done

A task is complete when:

- Code is moved/updated
- All imports are correct
- Tests pass for the affected code
- No compilation errors or warnings
- Code follows project style guidelines
- Changes are committed to git
