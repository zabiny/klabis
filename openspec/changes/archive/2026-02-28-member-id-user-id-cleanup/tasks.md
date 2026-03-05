# Domain Identity Types Cleanup - Implementation Tasks

## 1. Foundation

- [x] 1.1 Verify `MemberId.toUserId()` method exists and is public
- [x] 1.2 Verify `MemberId`, `UserId`, `EventId` are in module root packages with public visibility
- [x] 1.3 Move any typed ID classes not in module root to correct location
- [x] 1.4 Update `Member` aggregate: getId() returns `MemberId` instead of `UserId`
- [x] 1.5 Update `MemberRepository` interface to use `MemberId` for query methods
- [x] 1.6 Update `MemberJdbcRepository` implementation to convert UUID→MemberId for lookups
- [x] 1.7 Update `MemberMemento` toDomain() method to construct `MemberId` from UUID
- [x] 1.8 Write unit tests for `MemberId` value object (verify toUserId() works)

## 2. Members Module - Services

- [x] 2.1 Update `ManagementService` interface: updateMember() accepts `MemberId` instead of `UUID`
- [x] 2.2 Update `ManagementService` interface: terminateMember() accepts `MemberId` instead of `UUID`
- [x] 2.3 Update `ManagementServiceImpl` to use `MemberId` parameters
- [x] 2.4 Update `RegistrationService` interface: registerMember() returns `MemberId` instead of `UUID`
- [x] 2.5 Update `RegistrationServiceImpl` to return `MemberId`
- [x] 2.6 Update service method implementations that call `memberRepository.findById()` to pass `MemberId`

## 3. Members Module - Controllers

- [x] 3.1 Update `MemberController` update endpoints: convert `UUID` path variable to `MemberId` before calling service
- [x] 3.2 Update `MemberController` terminate endpoint: convert `UUID` path variable to `MemberId`
- [x] 3.3 Update `MemberController` getMember endpoint: convert `UUID` path variable to `MemberId`
- [x] 3.4 Update `RegistrationController` registerMember endpoint: handle `MemberId` return from service
- [x] 3.5 Update `MemberPermissionsLinkProcessor` to work with `MemberId` from response
- [x] 3.6 Update `MemberMapper` to handle `MemberId` in mapping methods

## 4. Members Module - Exceptions and Events

- [x] 4.1 Update `MemberCreatedEvent` to use `MemberId` field instead of `UUID`
- [x] 4.2 Update `MemberTerminatedEvent` to use `MemberId` for terminated member
- [x] 4.3 Update `MemberId` usage in event publishers
- [x] 4.4 Verify event handlers correctly handle typed IDs

## 5. Members Module - Tests

- [x] 5.1 Update `MemberTest` domain tests to use `MemberId`
- [x] 5.2 Update `ManagementServiceTest` to use `MemberId` parameters
- [x] 5.3 Update `RegistrationServiceTest` to expect `MemberId` return type
- [x] 5.4 Update `MemberControllerApiTest` to convert UUID→MemberId in test setups
- [x] 5.5 Update `MemberControllerSecurityTest` to use `MemberId`
- [x] 5.6 Update `MemberTestDataBuilder` to build with `MemberId`
- [x] 5.7 Update `MemberMappingTests` to use `MemberId`
- [x] 5.8 Update `MemberLifecycleE2ETest` to handle `MemberId`
- [x] 5.9 Update `MemberTerminationE2ETest` to handle `MemberId`
- [x] 5.10 Update `MemberPermissionsLinkProcessorTest` to use `MemberId`
- [x] 5.11 Update `RegisterMemberAutoProvisioningTest` to use `MemberId`
- [x] 5.12 Update integration tests that reference member IDs

## 6. Users Module

- [x] 6.1 Update `UserCreatedEvent` field from `UUID userId` to `UserId userId`
- [x] 6.2 Update `UserCreatedEvent` constructors to accept `UserId`
- [x] 6.3 Update `UserCreatedEvent.fromUser()` factory method
- [x] 6.4 Update `UserCreatedEvent.fromUserWithEmail()` factory method
- [x] 6.5 Update `UserCreatedEventTest` to use `UserId`
- [x] 6.6 Update event handlers that consume `UserCreatedEvent`
- [x] 6.7 Update `UserCreatedEventHandlerTest` to verify `UserId` usage

## 7. Events Module - Services

- [x] 7.1 Update `EventRegistrationService.registerMember()` to accept `EventId` and `MemberId`
- [x] 7.2 Update `EventRegistrationService.unregisterMember()` to accept `EventId` and `MemberId`
- [x] 7.3 Update `EventRegistrationService.listRegistrations()` to accept `EventId`
- [x] 7.4 Update `EventRegistrationService.getOwnRegistration()` to accept `EventId` and `MemberId`
- [x] 7.5 Update `EventManagementService.updateEvent()` to accept `EventId`
- [x] 7.6 Update `EventManagementService.publishEvent()` to accept `EventId`
- [x] 7.7 Update `EventManagementService.cancelEvent()` to accept `EventId`
- [x] 7.8 Update `EventManagementService.finishEvent()` to accept `EventId`
- [x] 7.9 Update `EventManagementService.getEvent()` to accept `EventId`

## 8. Events Module - Controllers

- [x] 8.1 Update `EventRegistrationController`: convert `UUID` path variables to `EventId` and `MemberId`
- [x] 8.2 Update `EventManagementController`: convert `UUID` path variable to `EventId`
- [x] 8.3 Update all controller methods that call event services

## 9. Events Module - Exceptions and Mementos

- [x] 9.1 Update `EventNotFoundException` to accept `EventId` instead of `UUID`
- [x] 9.2 Update `DuplicateRegistrationException` to accept `MemberId` and `EventId`
- [x] 9.3 Update `RegistrationNotFoundException` to accept `MemberId` and `EventId`
- [x] 9.4 Update all throw sites for these exceptions
- [x] 9.5 Verify `EventRegistrationMemento` continues using `UUID` (no change)
- [x] 9.6 Verify `EventMemento` continues using `UUID` (no change)

## 10. Events Module - Tests

- [x] 10.1 Update `EventRegistrationServiceTest` to use `EventId` and `MemberId`
- [x] 10.2 Update `EventManagementServiceTest` to use `EventId`
- [x] 10.3 Update `EventRegistrationControllerTest` to use typed IDs
- [x] 10.4 Update `EventControllerTest` to use `EventId`
- [x] 10.5 Update `EventTest` domain tests
- [x] 10.6 Update `EventRegistrationTest` domain tests

## 11. Calendar Module

- [x] 11.1 Update `CalendarMemento` repository methods to continue using `UUID` (verify no change needed)
- [x] 11.2 Update calendar services to convert `UUID`→`EventId` where event lookups occur
- [x] 11.3 Update `CalendarMemento` to use `UUID` for eventId field (verify current state)
- [x] 11.4 Update `CalendarJdbcRepository` to handle `UUID` eventId parameter
- [x] 11.5 Update `CalendarManagementServiceTest` to use typed IDs
- [x] 11.6 Update `CalendarControllerTest` to use typed IDs
- [x] 11.7 Update `CalendarRepositoryAdapterTest` to use typed IDs
- [x] 11.8 Update `CalendarItemTestDataBuilder` to support typed IDs

## 12. Test Infrastructure

- [x] 12.1 Update `JwtParams` to support `MemberId` in withMemberId() methods
- [x] 12.2 Update `JwtParamsTest` to verify `MemberId` support
- [x] 12.3 Update `WithKlabisMockUserSecurityContextFactory` to handle `MemberId`
- [x] 12.4 Update `CurrentUserIntegrationTest` to use typed IDs
- [x] 12.5 Update `KlabisAuthorizationServerCustomizerTest` to use typed IDs
- [x] 12.6 Update `EventPublishingIntegrationTest` to use typed IDs

## 13. DTOs and External API

- [x] 13.1 Verify `MemberDto` continues using `UUID` for API compatibility
- [x] 13.2 Verify `MemberDetailsResponse` continues using `UUID` for id field
- [x] 13.3 Verify `EventDto` continues using `UUID` for id field
- [x] 13.4 Verify controller DTOs use `UUID` (no changes to API contracts)

## 14. Final Verification

- [x] 14.1 Run full test suite and verify all tests pass
- [x] 14.2 Verify no compilation errors related to type mismatches
- [x] 14.3 Check that all typed ID classes are public and in module root packages
- [x] 14.4 Verify external API contracts unchanged (DTOs still use UUID)
- [x] 14.5 Run E2E tests to verify full application flow works
- [x] 14.6 Code review: verify consistent use of typed IDs across modules
