## 1. Authorization server depends on primary port (slice 1)

- [x] 1.1 In `KlabisUserDetailsService`, replace the `UserPermissionsRepository` constructor dependency with `PermissionService` (from `com.klabis.common.users.application`)
- [x] 1.2 Replace inlined `permissionsRepository.findById(user.getId()).orElse(UserPermissions.empty(user.getId()))` with `permissionService.getUserPermissions(user.getId())`
- [x] 1.3 Remove the `// TODO: hide permissions repository call behind UserService interface` comment and the now-unused `UserPermissionsRepository` import
- [x] 1.4 Update/verify `KlabisUserDetailsService` unit tests to mock `PermissionService` instead of `UserPermissionsRepository`; assert missing-permissions → empty authorities still holds
- [x] 1.5 Run authorizationserver + users tests (test-runner agent); commit slice

## 2. Move events cross-module ports to `events.application` (slice 2)

- [x] 2.1 Move `EventDataProvider` from `com.klabis.events` to `com.klabis.events.application` (interface + `@SecondaryPort`)
- [x] 2.2 Move `EventScheduleQuery` from `com.klabis.events` to `com.klabis.events.application`
- [x] 2.3 Create `events/application/package-info.java` with `@NamedInterface("application")` (mirror `finance/application/package-info.java`)
- [x] 2.4 Update implementations: `EventDataProviderImpl`, `EventScheduleQueryImpl` (package import only)
- [x] 2.5 Update consumers: `CalendarEventSyncService`, `CalendarManagementService`, `IcalFeedService` imports to `com.klabis.events.application.*`
- [x] 2.6 Update `calendar/package-info.java` javadoc / any `@NamedInterface` reference if it names the old port location (no change needed — javadoc names type, not package path)
- [x] 2.7 Run events + calendar + `ModuleStructureVerificationTest` (test-runner agent); commit slice

## 3. Move members financial-state port to `members.application` (slice 3)

- [ ] 3.1 Move `MemberFinancialStatePort` from `com.klabis.members` to `com.klabis.members.application`
- [ ] 3.2 Add `@NamedInterface("application")` to `members/application/package-info.java`
- [ ] 3.3 Update consumer `members.application.ManagementService` import
- [ ] 3.4 Update implementor `finance.application.MemberFinancialStateAdapter` import
- [ ] 3.5 Run members + finance + `ModuleStructureVerificationTest` (test-runner agent); commit slice

## 4. Verification & docs

- [ ] 4.1 Full Modulith verification passes (`ModuleStructureVerificationTest`, `FinanceModuleStructureTest`, `ModuleDocumentationTests`)
- [ ] 4.2 Confirm no remaining cross-module import of any `*Repository` and no cross-module port in a module root package
- [ ] 4.3 Update `backend-patterns` skill: document "cross-module ports live in `<module>.application`, exposed via `@NamedInterface`"
- [ ] 4.4 Add an ADR in `docs/design-decisions.md` recording the convention and the authorizationserver→primary-port rule
