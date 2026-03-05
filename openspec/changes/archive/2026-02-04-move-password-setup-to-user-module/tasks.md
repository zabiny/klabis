## 1. Domain Model Preparation

- [x] 1.1 Create `UserCreationParams` record with builder pattern in users module
    - Fields: username, passwordHash, authorities, email (optional)
    - Add static `builder()` method and nested Builder class
    - Add validation for required fields
- [x] 1.2 Add optional `email` field to `UserCreatedEvent` domain event
    - Add private final String email field (nullable)
    - Add `getEmail()` method returning `Optional<String>`
    - Keep existing `fromUser()` factory method (sets email to null)
- [x] 1.3 Add `fromUserWithEmail(User user, String email)` factory method to `UserCreatedEvent`
    - Sets all User fields plus email parameter
    - Makes PII flow explicit in code

## 2. UserService API Extension

- [x] 2.1 Add `createUserPendingActivation(UserCreationParams)` method to `UserService` interface
    - Keep existing method for backward compatibility
- [x] 2.2 Implement new method in `UserServiceImpl`
    - Delegate to existing method internally (extract common logic)
    - Use `fromUserWithEmail()` when params contain email
    - Use `fromUser()` when email is null
    - Publish UserCreatedEvent with email when provided
- [x] 2.3 Write unit tests for UserCreationParams builder
    - Test build with all fields including email
    - Test build without email (null)
    - Test validation of required fields
- [x] 2.4 Write unit tests for UserServiceImpl new method
    - Test with UserCreationParams containing email
    - Test with UserCreationParams without email
    - Verify UserCreatedEvent contains email when provided
    - Verify UserCreatedEvent email is null when not provided

## 3. Event Handler in Users Module

- [x] 3.1 Create `UserCreatedEventHandler` class in users module
    - Add `@Component` annotation
    - Add `@ApplicationModuleListener` to event handler method
- [x] 3.2 Implement password setup logic in UserCreatedEventHandler
    - Check if `event.getAccountStatus() == PENDING_ACTIVATION`
    - Check if `event.getEmail().isPresent()`
    - Fetch User from UserService
    - Call `passwordSetupService.generateToken(user)`
    - Call `passwordSetupService.sendPasswordSetupEmailWithUsername(username, email, token)`
    - Add warning log when email is absent
- [x] 3.3 Write unit tests for UserCreatedEventHandler
    - Test password setup sent when email present and PENDING_ACTIVATION
    - Test no password setup when email absent
    - Test no password setup when status is not PENDING_ACTIVATION
    - Mock PasswordSetupService and UserService

## 4. RegistrationService Update

- [x] 4.1 Update `RegistrationService.registerMember()` to use UserCreationParams builder
    - Replace direct UserService call with builder pattern:
      ```java
      UserCreationParams params = UserCreationParams.builder()
          .username(registrationNumber.getValue())
          .passwordHash(passwordHash)
          .authorities(Set.of(Authority.MEMBERS_READ))
          .email(email.value())
          .build();
      UserId sharedId = userService.createUserPendingActivation(params);
      ```
- [x] 4.2 Update integration tests for RegistrationService
    - Verify UserCreatedEvent is published with email field
    - Verify password setup email is sent during registration
    - Verify email greeting uses username (registration number)

## 5. MemberCreatedEventHandler Cleanup

- [x] 5.1 Remove password setup logic from `MemberCreatedEventHandler`
    - Remove PasswordSetupService dependency
    - Remove UserService dependency
    - Remove password setup method call
    - Keep only logging/audit functionality
- [x] 5.2 Update tests for MemberCreatedEventHandler
    - Remove password setup verification
    - Add verification that only logging occurs
    - Verify no interaction with PasswordSetupService

## 6. Email Template Update

- [x] 6.1 Update password setup email greeting to use username
    - Change from "Dear {firstName}" to "Dear {username}"
    - Ensure username (registration number) is passed to email template
    - Verify email template renders correctly

## 7. End-to-End Testing

- [x] 7.1 Verify E2E test for complete registration flow
    - Register new member with email
    - Verify UserCreatedEvent contains email
    - Verify password setup email is sent
    - Verify email contains username in greeting
    - Verify MemberCreatedEvent is logged
    - Existing `PasswordSetupFlowE2ETest` and `MemberRegistrationE2ETest` cover this
- [x] 7.2 Verify user creation without email scenario
    - Admin-created users without email work correctly
    - No password setup email sent when email absent
    - Backward compatibility maintained with old method signature
- [x] 7.3 Verify transaction rollback scenario
    - User creation failure rolls back Member creation
    - Member creation failure rolls back User creation
    - No events published on rollback
    - Existing transactional tests verify this

## 8. Documentation and Cleanup

- [x] 8.1 Update JavaDoc for UserService interface
    - Document new `createUserPendingActivation(UserCreationParams)` method
    - Note that email is optional and used for password setup
    - Done in UserService.java interface definition
- [x] 8.2 Update JavaDoc for UserCreationParams
    - Document builder pattern usage
    - Document email field purpose
    - Done in UserCreationParams record class
- [x] 8.3 Keep old `createUserPendingActivation(String, String, Set)` method
    - Maintained for backward compatibility
    - Delegates to new builder pattern method internally
    - Existing code and tests use this method without modification
- [x] 8.4 Run full test suite and verify coverage
    - All unit tests pass (UserCreationParamsTest, UserServiceImplTest, UserCreatedEventHandlerTest)
    - All integration tests pass (RegistrationServiceTest)
    - All E2E tests pass (PasswordSetupFlowE2ETest, MemberRegistrationE2ETest)
