## Why

Two architectural inconsistencies weaken the hexagonal/Modulith boundaries: (1) `KlabisUserDetailsService` in the `authorizationserver` module reaches into another module's **secondary port** (`users.domain.UserPermissionsRepository`) instead of consuming a **primary port** — the class even carries a `// TODO: hide permissions repository call behind UserService interface`; (2) cross-module ports live in inconsistent packages — some in `application` (the convention, e.g. `finance.application.*`, `users.application.PermissionService`), some in the module **root** package (`events.EventDataProvider`, `events.EventScheduleQuery`, `members.MemberFinancialStatePort`). Unifying both makes the dependency rules predictable and prevents future modules from depending on foreign repositories.

## What Changes

- **Authorization server stops depending on a foreign repository.** `KlabisUserDetailsService` replaces its `UserPermissionsRepository` dependency with the existing primary port `PermissionService.getUserPermissions(userId)`. The semantics are identical 1:1 — `PermissionServiceImpl.getUserPermissions()` is literally `permissionsRepository.findById(userId).orElse(UserPermissions.empty(userId))`, exactly the inlined logic removed from `KlabisUserDetailsService`. The `// TODO` comment is removed.
- **Cross-module ports moved into the `application` package** of their owning module, matching the established convention:
  - `com.klabis.events.EventDataProvider` → `com.klabis.events.application.EventDataProvider`
  - `com.klabis.events.EventScheduleQuery` → `com.klabis.events.application.EventScheduleQuery`
  - `com.klabis.members.MemberFinancialStatePort` → `com.klabis.members.application.MemberFinancialStatePort`
- Implementations and consumers updated to the new package (import-only changes): `EventDataProviderImpl`, `EventScheduleQueryImpl`, `MemberFinancialStateAdapter`, calendar services (`CalendarEventSyncService`, `CalendarManagementService`, `IcalFeedService`), `members.application.ManagementService`.
- `@NamedInterface` exposure adjusted so the moved ports stay visible to their existing consumers (Modulith encapsulation must keep allowing the same cross-module access, no more, no less).

> No item above changes API request/response shape, status codes, authorization outcomes, validation, or any business rule. All changes are package moves and a like-for-like dependency swap.

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/users-authentication/spec.md` — unaffected; describes login/authority behavior, never the internal `KlabisUserDetailsService`/repository wiring. The `getUserPermissions` swap is semantically identical (missing permissions → empty authorities, unchanged).
- `openspec/specs/users/spec.md`, `openspec/specs/member-permissions-dialog/spec.md` — unaffected; describe permissions from the user's perspective, not the port/package layout.
- `openspec/specs/calendar-items/spec.md`, `openspec/specs/events/spec.md`, `openspec/specs/member-accounts/spec.md` — unaffected; the moved ports keep the same contract and the same consumers; only their package changes.
- `openspec/specs/non-functional-requirements/spec.md` — unaffected; the cross-module domain-event requirement (publish events for integration) is untouched.

**Why no spec update is needed:**
This is a pure internal refactoring — package relocation of Java types plus a like-for-like dependency swap to an existing primary port with identical semantics. No user, API consumer, or integration test can observe the change. No requirement in any spec describes the affected classes, packages, or wiring.

## Impact

- **Modules:** `authorizationserver`, `events`, `members`, `calendar` (consumer-side imports only).
- **Code:** ~3 port interfaces moved, their implementations + ~6 consumer classes re-imported, 1 dependency swap, `@NamedInterface`/`package-info.java` adjustments.
- **Tests:** existing Modulith verification tests (`ModuleStructureVerificationTest`, `FinanceModuleStructureTest`) and unit tests must keep passing; any test referencing the old package paths is updated.
- **Developer workflow:** clearer convention — primary/secondary cross-module ports always live in `<module>.application`; no behavioral or build-process change.
- **Docs:** `backend-patterns` skill and an ADR in `docs/design-decisions.md` should record the "cross-module ports live in `application`" convention.
