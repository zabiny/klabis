## 1. User Aggregate Changes

- [ ] 1.1 Add `suspend()` method to User aggregate that returns new User with AccountStatus.SUSPENDED and enabled=false
- [ ] 1.2 Add `reactivate()` method to User aggregate that returns new User with AccountStatus.ACTIVE and enabled=true
- [ ] 1.3 Add unit tests for User.suspend() verifying status change and immutability
- [ ] 1.4 Add unit tests for User.reactivate() verifying status change and immutability
- [ ] 1.5 Add unit test for isAuthenticatable() returning false when suspended

## 2. Member Aggregate Changes

- [ ] 2.1 Create MemberReactivatedEvent domain class with eventId, memberId, registrationNumber, reactivatedAt, reactivatedBy
- [ ] 2.2 Add ReactivateMembership command record to Member aggregate
- [ ] 2.3 Add handle(ReactivateMembership) command to Member aggregate (sets active=true, clears deactivation fields, publishes event)
- [ ] 2.4 Add unit test for Member reactivation command handler
- [ ] 2.5 Add unit test for MemberReactivatedEvent.fromMember() factory method

## 3. Event Handlers in Users Module

- [ ] 3.1 Create users/integration package
- [ ] 3.2 Create MemberTerminatedEventHandler with @ApplicationModuleListener and @Component
- [ ] 3.3 Implement onMemberTerminated() method: find User by username, call suspend(), save
- [ ] 3.4 Create MemberReactivatedEventHandler with @ApplicationModuleListener and @Component
- [ ] 3.5 Implement onMemberReactivated() method: find User by username, call reactivate(), save
- [ ] 3.6 Add idempotent check (skip if already in target state)
- [ ] 3.7 Add integration test for MemberTerminatedEventHandler using real Spring context
- [ ] 3.8 Add integration test for MemberReactivatedEventHandler using real Spring context
- [ ] 3.9 Add integration test for missing User account scenario (graceful handling)

## 4. E2E Test Updates

- [ ] 4.1 Verify MemberLifecycleE2ETest STEP 11 now passes (terminated user denied API access)
- [ ] 4.2 Add E2E test scenario for Member reactivation with User reactivation verification

## 5. Documentation

- [ ] 5.1 Update users/spec.md with ADDED requirements for suspension, reactivation, event integration
- [ ] 5.2 Update member-termination/spec.md with MODIFIED requirement for event publishing and ADDED reactivation requirement
- [ ] 5.3 Update MemberTerminatedEvent javadoc to mention User suspension behavior
- [ ] 5.4 Add javadoc to new event handlers explaining cross-module integration
