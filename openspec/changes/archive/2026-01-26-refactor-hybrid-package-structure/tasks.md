# Tasks: Refactor to Hybrid Package Structure

**Note**: Following KISS simplification (Phase 1), Command/Query handlers have been consolidated into Services. This
migration focuses on reorganizing the simpler Service-based architecture.

## Overall Progress

**Status**: ✅ **BOTH MODULES COMPLETE** | Build Status: ✅ **SUCCESS**

### Module Refactoring Summary:

- ✅ **Members Module**: 62 files migrated to hybrid structure (Commit: `fbf7115`)
- ✅ **Users Module**: 48 files migrated to hybrid structure (2025-01-26)

### Package Structure Achieved:

```
com.klabis.{module}/
├── model/                    # Shared domain model (@NamedInterface)
├── {feature}/                # Feature packages (@NamedInterface)
│   ├── *Request.java         # DTOs directly in feature package
│   ├── *Response.java        # DTOs directly in feature package
│   └── *Exception.java       # Exceptions directly in feature package
├── persistence/jdbc/         # Shared persistence (internal)
└── shared/util/              # Shared utilities (internal)
```

### Next Steps:

- ⏳ Run full test suite to verify all tests pass
- ⏳ Cross-module integration testing
- ⏳ Final code review
- ⏳ Update documentation

---

## 1. Members Module Migration ✅ **COMPLETED**

**Completion Date**: 2025-01-26
**Commit**: `fbf7115`
**Test Results**: 366/366 tests passing (100%)

### 1.1 Create New Package Structure ✅

- [x] 1.1.1 Create `members/model/` package
- [x] 1.1.2 Create `members/model/validation/` subpackage
- [x] 1.1.3 Create `members/registration/` package
- [x] 1.1.4 Create `members/management/` package
- [x] 1.1.8 Create `members/persistence/` package
- [x] 1.1.9 Create `members/persistence/jdbc/` subpackage
- [x] 1.1.10 Create `members/persistence/jdbc/converters/` subpackage
- [x] 1.1.11 Create `members/shared/` package
- [x] 1.1.12 Create `members/shared/validation/` subpackage

### 1.2 Move Domain Model to model/ Package ✅

- [x] 1.2.1 Move `PersonalInformation.java` to `model/`
- [x] 1.2.2 Move `Address.java` to `model/`
- [x] 1.2.3 Move `PersonName.java` to `model/`
- [x] 1.2.4 Move `EmailAddress.java` to `model/`
- [x] 1.2.5 Move `PhoneNumber.java` to `model/`
- [x] 1.2.6 Move `IdentityCard.java` to `model/`
- [x] 1.2.7 Move `TrainerLicense.java` to `model/`
- [x] 1.2.8 Move `MedicalCourse.java` to `model/`
- [x] 1.2.9 Move `GuardianInformation.java` to `model/`
- [x] 1.2.10 Move `RegistrationNumber.java` to `model/`
- [x] 1.2.11 Move `RegistrationNumberGenerator.java` to `model/`
- [x] 1.2.12 Move `Nationality.java` to `model/`
- [x] 1.2.13 Move `Gender.java` to `model/`
- [x] 1.2.14 Move `DocumentType.java` to `model/`
- [x] 1.2.15 Move `DrivingLicenseGroup.java` to `model/`
- [x] 1.2.16 Move `ExpiringDocument.java` to `model/`
- [x] 1.2.17 Move `AuditMetadata.java` to `model/`
- [x] 1.2.18 Move `validation/` package contents to `model/validation/`
- [x] 1.2.19 Update package declarations in all model classes
- [x] 1.2.20 Create `model/package-info.java` with `@NamedInterface("domain-model")`

### 1.3 Move Key Types to Module Root ✅

- [x] 1.3.1 Move `Member.java` to module root
- [x] 1.3.2 Move `MemberRepository.java` to module root
- [x] 1.3.3 Move `MemberNotFoundException.java` to module root
- [x] 1.3.4 Move `MemberCreatedEvent.java` to module root
- [x] 1.3.5 Verify package declarations

### 1.4 Move Registration Feature Code ✅

- [x] 1.4.1 Create `registration/RegistrationService.java` with `registerMember()` method (230 lines)
    - [x] Extract registration logic from current `MemberService.registerMember()`
    - [x] Add `@Service` annotation
    - [x] Inject `MemberRepository` and `UserRepository`
    - [x] Keep `@Transactional` annotations
- [x] 1.4.2 Move `MemberCreatedEventHandler.java` to `registration/`
- [x] 1.4.3 Move `MemberRegistrationResponse.java` to `registration/`
- [x] 1.4.4 Move `RegisterMemberRequest.java` from presentation to `registration/`
- [x] 1.4.5 Update package declarations in registration classes
- [x] 1.4.6 Create `registration/package-info.java` with `@NamedInterface("registration")`
- [x] 1.4.7 Update `MemberController` to inject `RegistrationService` instead of `MemberService` for registration
  endpoint

### 1.5 Move Management Feature Code ✅

- [x] 1.5.1 Move `MemberController.java` to `management/`
- [x] 1.5.2 Create `management/ManagementService.java` with `getMember()`, `updateMember()`, `listMembers()` methods (
  539 lines)
    - [x] Extract management logic from current `MemberService`
    - [x] Add `@Service` annotation
    - [x] Inject `MemberRepository` and `UserRepository`
    - [x] Keep `@Transactional` annotations
- [x] 1.5.3 Move `AddressRequest.java` to `management/`
- [x] 1.5.4 Move `AddressResponse.java` to `management/`
- [x] 1.5.5 Move `UpdateMemberRequest.java` to `management/`
- [x] 1.5.6 Move `MemberDetailsDTO.java` to `management/`
- [x] 1.5.7 Move `MemberSummaryDTO.java` to `management/`
- [x] 1.5.8 Move `MemberDetailsResponse.java` to `management/`
- [x] 1.5.9 Move `GuardianDTO.java` to `management/`
- [x] 1.5.10 Move `IdentityCardDto.java` to `management/`
- [x] 1.5.11 Move `MedicalCourseDto.java` to `management/`
- [x] 1.5.12 Move `TrainerLicenseDto.java` to `management/`
- [x] 1.5.13 Move `SelfEditNotAllowedException.java` to `management/`
- [x] 1.5.14 Move `AdminFieldAccessException.java` to `management/`
- [x] 1.5.15 Move `InvalidUpdateException.java` to `management/`
- [x] 1.5.16 Move `UserIdentificationException.java` to `management/`
- [x] 1.5.17 Update package declarations in management classes
- [x] 1.5.18 Create `management/package-info.java` with `@NamedInterface("management")`
- [x] 1.5.19 Update `MemberController` to inject `ManagementService` instead of `MemberService`

### 1.6 Delete Old Unified Service ✅

- [x] 1.6.1 Verify `RegistrationService` and `ManagementService` are working correctly
- [x] 1.6.2 Delete old `application/MemberService.java` (deleted, converted to ManagementService)
- [x] 1.6.3 Verify no other code references old `MemberService`
- [x] 1.6.4 Run tests to ensure migration successful (366/366 tests passing)

### 1.7 Move Persistence Code ✅

- [x] 1.7.1 Move `infrastructure/jdbcrepository/` to `persistence/jdbc/`
- [x] 1.7.2 Move `MemberJdbcRepository.java` to `persistence/jdbc/`
- [x] 1.7.3 Move `MemberRepositoryJdbcImpl.java` to `persistence/jdbc/`
- [x] 1.7.4 Move `MemberMemento.java` to `persistence/jdbc/`
- [x] 1.7.5 Move `converters/` to `persistence/jdbc/converters/`
- [x] 1.7.6 Update package declarations in persistence classes
- [x] 1.7.7 Create `persistence/package-info.java` (no @NamedInterface)
- [x] 1.7.8 Create `persistence/jdbc/package-info.java` (no @NamedInterface)

### 1.8 Move Shared Code ✅

- [x] 1.8.1 Move `SortOrder.java` to `shared/`
- [x] 1.8.2 Move `presentation/validation/` to `shared/validation/`
- [x] 1.8.3 Update package declarations in shared classes

### 1.9 Update All Import Statements ✅

- [x] 1.9.1 Update imports in `registration/RegistrationService.java`
- [x] 1.9.2 Update imports in `management/ManagementService.java`
- [x] 1.9.3 Update imports in `management/MemberController.java`
- [x] 1.9.4 Update imports in `registration/MemberCreatedEventHandler.java`
- [x] 1.9.5 Update imports in `MemberRepositoryJdbcImpl.java`
- [x] 1.9.6 Update imports in all converter classes
- [x] 1.9.7 Update imports in all validator classes
- [x] 1.9.8 Update imports in other modules that reference members classes
- [x] 1.9.9 Verify no broken imports remain (zero compilation errors)

### 1.10 Update Module Root package-info.java ✅

- [x] 1.10.1 Verify `members/package-info.java` has `@ApplicationModule`
- [x] 1.10.2 Add documentation about new package structure

### 1.11 Delete Old Empty Packages ✅

- [x] 1.11.1 Delete empty `application/` package
- [x] 1.11.2 Delete empty `domain/` package
- [x] 1.11.3 Delete empty `infrastructure/` package
- [x] 1.11.4 Delete empty `presentation/` package

### 1.12 Migrate Members Tests ✅

- [x] 1.12.1 Create test package structure mirroring new source structure
- [x] 1.12.2 Move `model/` tests to `model/` test package
- [x] 1.12.3 Split `MemberServiceTest` into feature-specific tests:
    - [x] 1.12.3.1 Create `RegistrationServiceTest` with registration tests (6 tests)
    - [x] 1.12.3.2 Create `ManagementServiceTest` with query/update tests (16 tests)
- [x] 1.12.4 Move `MemberController` tests to `management/` test package
- [x] 1.12.5 Move `persistence/` tests to `persistence/` test package
- [x] 1.12.6 Update all test import statements
- [x] 1.12.7 Update all test package declarations
- [x] 1.12.8 Delete old `MemberServiceTest` after splitting

### 1.13 Verify Members Module ✅

- [x] 1.13.1 Run all members unit tests (366/366 passing)
- [x] 1.13.2 Run all members integration tests (all passing)
- [x] 1.13.3 Verify Spring Modulith tests for members module pass
- [x] 1.13.4 Verify `mvn compile` succeeds for members module (zero errors)
- [x] 1.13.5 Verify no import errors in IDE

**Summary**: Members module successfully migrated to hybrid package structure. All 62 source files reorganized, services
split, tests aligned. Committed as `fbf7115`.

## 2. Users Module Migration ✅ **COMPLETED**

**Completion Date**: 2025-01-26
**Status**: Hybrid package structure fully implemented
**Build Result**: ✅ BUILD SUCCESS (zero compilation errors)

### 2.1 Create New Package Structure ✅

- [x] 2.1.1 Create `users/model/` package
- [x] 2.1.2 Create `users/model/validation/` subpackage (not needed - no separate validation)
- [x] 2.1.3 Create `users/authentication/` package
- [x] 2.1.4 Create `users/passwordsetup/` package
- [x] 2.1.6 Create `users/persistence/` package
- [x] 2.1.7 Create `users/persistence/jdbc/` subpackage
- [x] 2.1.8 Create `users/shared/util/` subpackage

### 2.2 Move Domain Model to model/ Package ✅

- [x] 2.2.1 Move `AccountStatus.java` to `model/`
- [x] 2.2.2 Move `Authority.java` to `model/`
- [x] 2.2.3 Move `ActivationToken.java` to `model/`
- [x] 2.2.4 Move `TokenHash.java` to `model/`
- [x] 2.2.5 Move `UserAuditMetadata.java` to `model/`
- [x] 2.2.6 Move `AuthorityValidator.java` to `model/`
- [x] 2.2.7 Move `PasswordSetupToken.java` to `model/` (shared domain)
- [x] 2.2.8 Move `PasswordSetupTokenRepository.java` to `model/` (shared domain)
- [x] 2.2.9 Update package declarations in all model classes
- [x] 2.2.10 Create `model/package-info.java` with `@NamedInterface("domain-model")`

### 2.3 Move Key Types to Module Root ✅

- [x] 2.3.1 Keep `User.java` at module root
- [x] 2.3.2 Move `UserRepository.java` to model/ (not root - consistent with architecture)
- [x] 2.3.3 Keep `UserId.java` in model/ (value object)
- [x] 2.3.4 Keep `UserNotFoundException.java` in authentication/
- [x] 2.3.5 Move `UserCreatedEvent.java` to `model/`
- [x] 2.3.6 Verify package declarations

### 2.4 Move Authentication Feature Code ✅

- [x] 2.4.1 Move `UserService.java` to `authentication/` (service already cohesive, no split needed)
- [x] 2.4.2 Move `PasswordSetupEventListener.java` to `authentication/`
- [x] 2.4.3 Move `PasswordSetupService.java` to `passwordsetup/` (separate feature)
- [x] 2.4.4 Move `KlabisUserDetailsService.java` to `authentication/`
- [x] 2.4.5 Move `UserController.java` to `authentication/`
- [x] 2.4.6 Move `RateLimiterConfiguration.java` to `passwordsetup/`
- [x] 2.4.7 Move `TokenCleanupJob.java` to `passwordsetup/`
- [x] 2.4.8 Move `PasswordSetupController.java` to `passwordsetup/`
- [x] 2.4.9 Move `PermissionsResponse.java` to `authentication/`
- [x] 2.4.10 Move `PermissionsResponseModel.java` to `authentication/`
- [x] 2.4.11 Move `PermissionsResponseModelAssembler.java` to `authentication/`
- [x] 2.4.12 Move `CannotRemoveLastPermissionManagerException.java` to `authentication/`
- [x] 2.4.13 Move `UserNotFoundException.java` to `authentication/`
- [x] 2.4.14 Update package declarations in all classes
- [x] 2.4.15 Create `authentication/package-info.java` with `@NamedInterface("authentication")`
- [x] 2.4.16 Create `passwordsetup/package-info.java` with `@NamedInterface("passwordsetup")`

### 2.5 Extract Inner Classes ✅

- [x] 2.5.1 Extract 8 DTOs from `PasswordSetupService` to `passwordsetup/`:
    - [x] `GeneratedTokenResult.java`
    - [x] `PasswordSetupRequest.java`
    - [x] `PasswordSetupResponse.java`
    - [x] `ValidateTokenResponse.java`
- [x] 2.5.2 Extract 4 DTOs from `PasswordSetupController` to `passwordsetup/`:
    - [x] `ErrorResponse.java`
    - [x] `SetPasswordRequest.java`
    - [x] `TokenRequestRequest.java`
    - [x] `TokenRequestResponse.java`
- [x] 2.5.3 Extract 4 exceptions from `PasswordSetupService` to `passwordsetup/`:
    - [x] `TokenValidationException.java` (base class)
    - [x] `TokenExpiredException.java`
    - [x] `TokenAlreadyUsedException.java`
    - [x] `PasswordValidationException.java`

### 2.6 Move Persistence Code ✅

- [x] 2.6.1 Move `infrastructure/jdbcrepository/` to `persistence/jdbc/`
- [x] 2.6.2 Move all User persistence files to `persistence/jdbc/`
- [x] 2.6.3 Move all PasswordSetupToken persistence files to `persistence/jdbc/` (shared)
- [x] 2.6.4 Update package declarations in persistence classes
- [x] 2.6.5 Create `persistence/package-info.java` (no @NamedInterface - internal)
- [x] 2.6.6 Create `persistence/jdbc/package-info.java` (no @NamedInterface - internal)

### 2.7 Create Shared Utilities ✅

- [x] 2.7.1 Create `shared/util/` package
- [x] 2.7.2 Extract `EmailUtil.maskEmail()` from `PasswordSetupService`
- [x] 2.7.3 Update `PasswordSetupService` to call `EmailUtil.maskEmail()`
- [x] 2.7.4 Verify compilation succeeds

### 2.8 Update All Import Statements ✅

- [x] 2.8.1 Update imports in all authentication classes
- [x] 2.8.2 Update imports in all passwordsetup classes
- [x] 2.8.3 Update imports in all persistence classes
- [x] 2.8.4 Update imports in `MemberCreatedEventHandler` (Members module)
- [x] 2.8.5 Verify no broken imports remain (BUILD SUCCESS)

### 2.9 Delete Old Empty Packages ✅

- [x] 2.9.1 Delete empty `application/` package
- [x] 2.9.2 Delete empty `domain/` package
- [x] 2.9.3 Delete empty `infrastructure/` package (including jdbcrepository/ and security/)
- [x] 2.9.4 Delete empty `presentation/` package

### 2.10 Verify Users Module ✅

- [x] 2.10.1 Verify `mvn compile` succeeds ✅ BUILD SUCCESS
- [x] 2.10.2 Verify zero compilation errors in Users module
- [x] 2.10.3 Verify package structure matches hybrid architecture
- [x] 2.10.4 Verify all files moved correctly (48 production files)
- [x] 2.10.5 Verify cross-module dependencies work (Members → Users)

**Summary**: Users module successfully migrated to hybrid package structure. All 48 production files reorganized, inner
classes extracted, shared utilities created, old packages deleted. BUILD SUCCESS with zero compilation errors.

## 3. Cross-Module Validation

### 3.1 Update Inter-Module Imports

- [ ] 3.1.1 Verify imports from other modules to members
- [ ] 3.1.2 Verify imports from other modules to users
- [ ] 3.1.3 Update any cross-module dependencies
- [ ] 3.1.4 Verify event handlers work across modules

### 3.2 Full Test Suite

- [ ] 3.2.1 Run complete unit test suite
- [ ] 3.2.2 Run complete integration test suite
- [ ] 3.2.3 Run Spring Modulith tests (`@ApplicationModuleTest`)
- [ ] 3.2.4 Verify `mvn clean install` succeeds
- [ ] 3.2.5 Verify application starts successfully
- [ ] 3.2.6 Run manual smoke tests on API endpoints

### 3.3 Documentation

- [ ] 3.3.1 Update `CONTRIBUTING.md` with new package structure guidelines
- [ ] 3.3.2 Update team documentation
- [ ] 3.3.3 Create migration guide for reference

## 4. Final Verification

- [ ] 4.1 Verify all tests pass
- [ ] 4.2 Verify no compilation warnings
- [ ] 4.3 Verify no IDE errors
- [ ] 4.4 Verify Spring Modulith documentation generation works
- [ ] 4.5 Code review of all changed files
- [ ] 4.6 Merge to main branch
