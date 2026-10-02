## Why

REST adapter tests are slow and fragmented: every `@WebMvcTest(controllers=...)` class with its own `@MockitoBean` set builds a separate Spring context, and controller behavior is spread over controller, security, integration and link-processor tests. A proof of concept in the `members` module (commits `1a67e6ba`, `9bc1841b`, `e6da643e`, `01824c12`) cut Spring contexts from 16 to 8 and test time from 121 s to 97 s while making the mock rules explicit (mock only primary ports, optional beans mocked locally). The remaining modules still use the legacy form (32 test classes use `@WithPostprocessors`, 41 use `@WebMvcTest`).

## What Changes

- Add a per-module `@<Module>WebMvcTest` meta-annotation (`@WebMvcTest` + `@ModuleSlicing(mode = STANDALONE)`, `extraIncludes` only for modules whose postprocessors or ports affect the responses) so all controller tests of a module share one Spring context. Modules: `events`, `finance`, `membershipfees`, `groups`, `calendar`, `sync`, `oris`, `common`.
- Make each module own a `<Module>WebMvcMockitoBeans` meta-annotation that mocks all its primary ports; other modules compose it instead of listing ports.
- Fix places where REST adapters depend on something other than primary ports, so mocks of non-primary beans disappear:
  - `events`: `AccommodationListCsvRenderer` (adapter bean mocked in tests).
  - `finance`: `MemberAccountRepository` used by `MemberAccountController`; `FinanceAccountLinkSupport` implemented in the adapter package.
  - `sync`: `SyncProjectionFieldReader` used by `SynchronizationController` and `SyncStateResponseConverter`.
  - `membershipfees`: `EventTypeOptionsPort`, `RankingOptionsPort` are only `@Port`, used by `MembershipFeeTierController`.
  - `common.settings`: `OrisClubKeyPort` is a secondary port; add a dedicated primary port over it.
  - `oris`: `OrisApiClient` stays as a documented exception (external client used directly by `OrisController`).
- Optional / feature-flag / profile-gated beans (e.g. `OrisEventImportPort`) are never part of group mocks. Tests that need them mock them locally; a sibling test without the mock covers the "disabled" variant.
- Merge security, integration and link-processor/postprocessor tests of a controller into its `@WebMvcTest` class (as `@Nested` groups), adapting expectations to the controller level. E2E tests are not touched (moved to the module root package where they are not already).
- Write 403 tests with stubbed port returns and verify them once at the end by mutation (suppress `@HasAuthority` emission in `api.mustache`) so each fails on the status assertion, not on an NPE or a different mechanism.
- Remove the legacy `@WithPostprocessors` and the per-test `@MockitoBean` form once all modules are migrated.
- Close open items from the members PoC: unit test for the `addsNoLinkWithoutContext` link-processor branch, confirm `POST /api/members` returning 201 is covered end-to-end elsewhere, reconsider the package-private `@Import(CalendarInfrastructureConfiguration)` coupling in `CalendarWebMvcMockitoBeans`.
- Update the `backend-patterns` skill where the migration changes guidance (legacy form removed).

## No Behavior Change Justification

Only test code and internal wiring change. Production edits are limited to adding primary-port methods that delegate to existing logic, annotating existing types as primary ports, and replacing direct repository or helper use in adapters with those ports. Request and response shapes, status codes, validation, authorization and HAL links stay identical; the existing assertions are kept and verified to pass in the migrated tests.

**Specs reviewed:**
- `openspec/specs/members/spec.md`, `legal-guardians/spec.md`, `member-accounts/spec.md` — unaffected, only test structure around them changes
- `openspec/specs/events/spec.md`, `event-registrations/spec.md`, `event-types/spec.md`, `event-categories/spec.md`, `disciplines/spec.md` — unaffected
- `openspec/specs/membership-fees/spec.md`, `data-synchronization/spec.md`, `member-synchronization/spec.md`, `oris-club-key/spec.md`, `calendar-items/spec.md`, `user-groups/spec.md` — unaffected

**Why no spec update is needed:**
The change restructures tests and the dependency direction of REST adapters (adapter → primary port). No requirement or scenario is added, modified or removed.

## Impact

- **Code:** `backend/src/test/**` for modules listed above; small production changes in `events`, `finance`, `sync`, `membershipfees`, `common.settings` (new or extended primary ports).
- **Build/runtime:** fewer Spring contexts in the test suite; shorter backend test run.
- **Developer workflow:** one documented way to write REST adapter tests (`backend-patterns` skill); the legacy `@WithPostprocessors` form disappears.
- **Verification:** final full run with `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true --rerun-tasks`; `ModularEventsTest` and `EventLoggingTests` fail on clean `main` and are not regressions.
