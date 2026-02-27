## 1. User Aggregate Changes

- [ ] 1.1 Add `suspend()` method to User aggregate that returns new User with AccountStatus.SUSPENDED and enabled=false
- [ ] 1.2 Add `reactivate()` method to User aggregate that returns new User with AccountStatus.ACTIVE and enabled=true
- [ ] 1.3 Add unit tests for User.suspend() verifying status change and immutability
- [ ] 1.4 Add unit tests for User.reactivate() verifying status change and immutability
- [ ] 1.5 Add unit test for isAuthenticatable() returning false when suspended

## 2. UserService Changes

- [ ] 2.1 Add `suspendUser(UserId userId)` method to UserService that finds User by ID and calls suspend()
- [ ] 2.2 Add `reactivateUser(UserId userId)` method to UserService that finds User by ID and calls reactivate()
- [ ] 2.3 Add idempotent check in suspendUser() - skip if User is already SUSPENDED
- [ ] 2.4 Add idempotent check in reactivateUser() - skip if User is already ACTIVE
- [ ] 2.5 Add graceful handling of missing User account (log warning, no error)
- [ ] 2.6 Add unit test for UserService.suspendUser() with existing User
- [ ] 2.7 Add unit test for UserService.reactivateUser() with existing User
- [ ] 2.8 Add unit test for idempotent suspendUser() (already suspended)
- [ ] 2.9 Add unit test for idempotent reactivateUser() (already active)
- [ ] 2.10 Add unit test for suspendUser() with non-existent User
- [ ] 2.11 Add unit test for reactivateUser() with non-existent User

## 3. Member Aggregate Changes

- [ ] 3.1 Create MemberReactivatedEvent domain class with eventId, memberId, registrationNumber, reactivatedAt, reactivatedBy
- [ ] 3.2 Add ReactivateMembership command record to Member aggregate
- [ ] 3.3 Add handle(ReactivateMembership) command to Member aggregate (sets active=true, clears deactivation fields, publishes event)
- [ ] 3.4 Add unit test for Member reactivation command handler
- [ ] 3.5 Add unit test for MemberReactivatedEvent.fromMember() factory method

## 4. MemberService Changes

- [ ] 4.1 Modify MemberService.terminateMember() to call UserService.suspendUser(member.getId().toUserId()) after saving terminated Member
- [ ] 4.2 Add MemberService.reactivateMember() method that calls Member.handle(ReactivateMembership) and UserService.reactivateUser(member.getId().toUserId())
- [ ] 4.3 Add integration test for terminateMember() verifying User suspension
- [ ] 4.4 Add integration test for reactivateMember() verifying User reactivation
- [ ] 4.5 Add integration test for terminateMember() with missing User account (graceful handling)

## 5. E2E Test Updates

- [ ] 5.1 Verify MemberLifecycleE2ETest STEP 11 now passes (terminated user denied API access)
- [ ] 5.2 Add E2E test scenario for Member reactivation with User reactivation verification

## 6. Documentation

- [ ] 6.1 Update users/spec.md with ADDED requirements for suspension, reactivation, UserService methods
- [ ] 6.2 Update member-termination/spec.md with MODIFIED requirement for User suspension and ADDED reactivation requirement
- [ ] 6.3 Update MemberTerminatedEvent javadoc to mention User suspension behavior via UserService
- [ ] 6.4 Add javadoc to UserService.suspendUser() and reactivateUser() methods
