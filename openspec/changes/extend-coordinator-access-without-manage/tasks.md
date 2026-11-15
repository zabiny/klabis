# Tasks

Each group is an end-to-end slice (spec → backend → frontend → tests → docs) following Red-Green-Refactor. Backend tests via the `test-runner` agent, frontend verified with `npm run build` + Vitest.

## 1. Coordinator sees their own DRAFT event (#294)

- [x] 1.1 Write failing `AuthorizationEvaluator` unit tests for `targetIdsOf(Authority, TargetType)` (returns ids of SPECIFIC grants of that type; empty without grants; ignores targets of another type), then implement by delegating to `AuthorizationSnapshot.targetsOf`; verify tests pass
- [x] 1.2 Document `targetIdsOf` in the `backend-patterns` skill (`authorization.md`: when to use it — restricting list queries to granted targets — and that modules must not re-derive relationships for access control); verify the skill reference mentions it
- [x] 1.3 Write failing `EventFilter` unit tests for `withDraftsLimitedTo(Set<EventId>)` (null = unrestricted, empty = no DRAFT, other statuses unaffected), then add the criterion; verify tests pass
- [x] 1.4 Write failing repository-adapter integration test (DRAFT limited to the given ids; empty set hides all DRAFT; combined with `status=DRAFT` and with `cancelledVisibleTo`), then translate `draftsLimitedTo` in `EventRepositoryAdapter` to `status <> DRAFT OR id IN (:ids)`; verify test passes
- [x] 1.5 Write failing `EventManagementService` tests (non-manager list applies `draftsLimitedTo` from the passed draft visibility instead of excluding DRAFT; detail returns DRAFT when "may see this draft"), then replace the `canManageEvents` boolean parameters; verify tests pass
- [x] 1.6 Write failing `EventController` `@WebMvcTest`s with `@WithKlabisMockUser(targetGrants = EVENTS_EDIT over the event)`: DRAFT detail returns the event to its coordinator and 404 to a non-coordinator; list for a coordinator contains their DRAFT and not another DRAFT; then compute draft visibility from `has(EVENTS_MANAGE)` / `targetIdsOf(EVENTS_EDIT, EVENT)` and `isAllowed([EVENTS_MANAGE, EVENTS_EDIT], event)`; verify tests pass and `AuthorizationArchitectureTest` stays green
- [x] 1.7 Update `events.yaml` descriptions of `listEvents` / `getEvent` (DRAFT visible to coordinators); verify `npm run openapi` and backend compile succeed

## 2. Coordinator publishes and cancels their own event

- [x] 2.1 Write failing `@WebMvcTest`s: coordinator (EVENTS_EDIT over event) can publish a DRAFT and cancel a DRAFT/ACTIVE event; coordinator of another event gets 403; detail and list row of a coordinated DRAFT event offer `publishEvent` and `cancelEvent` templates, a non-coordinated one offers none
- [x] 2.2 Change `publishEvent` and `cancelEvent` to `x-klabis-authority: [EVENTS_MANAGE, EVENTS_EDIT]` in `events.yaml` (+ template descriptions), regenerate backend API and frontend types; verify tests from 2.1 pass
- [x] 2.3 Verify in the frontend (Vitest on event detail / events table with a coordinator fixture) that "Publikovat" and "Zrušit" render from the templates without frontend code changes; fix only if they do not
- [x] 2.4 Amend ADR-010 in `docs/design-decisions.md` in place: the event-coordinator paragraph (coordinators may publish and cancel their events; changing coordinators, ORIS import and sync stay `EVENTS_MANAGE`) and the evaluator section (`targetIdsOf` beside `has` / `isSelf`, for restricting list queries); verify no sentence still says coordinators must not publish/cancel
- [x] 2.5 Update javadoc of `EventCoordinatorRelationshipSource` and "Event coordinator grants" in `backend/CLAUDE.md`; verify both say publish/cancel are included and coordinator changes/ORIS/sync stay EVENTS:MANAGE

## 3. Coordinators can apply category presets (#373)

- [x] 3.1 Write failing tests: member with only EVENTS_READ can list presets (`listPresets`); `getPreset` and preset mutations still require EVENTS_MANAGE; root resource contains `category-presets` only for EVENTS_MANAGE; events list and event detail contain a `categoryPresets` link
- [x] 3.2 Change `listPresets` to `x-klabis-authority: EVENTS_READ`, add explicit `has(EVENTS_MANAGE)` check to the root link in `EventsRootPostprocessor`, add `categoryPresets` links in `EventListPostprocessor` and `EventDetailsPostprocessor`, document them in `events.yaml` `x-hal-links`; regenerate; verify tests from 3.1 pass
- [x] 3.3 Write failing Vitest for `CategoryPresetPickerButton`: no request and no button when the page resource lacks `categoryPresets`; loads presets from the link href when present; then implement via `useHalRoute().resourceData`; verify tests pass and `npm run build` succeeds
- [x] 3.4 Verify the admin menu still hides "Šablony kategorií" for a regular member (existing `useRootNavigation`/Layout tests or a new one) and the presets page stays managers-only

## 4. "Vedoucí" filter in the events list (#295)

- [x] 4.1 Write failing `@WebMvcTest`: events list contains `coordinatorOptions` link to member options for a member with MEMBERS_READ; then add it in `EventListPostprocessor` and to `events.yaml` `x-hal-links`; regenerate; verify test passes
- [x] 4.2 Write failing Vitest for `EventsFilterBar`: "Vedoucí" select is rendered only when `coordinatorOptions` is available, lists the options, and selecting/clearing a member emits the change; then implement (plain select, label from `labels.ts`); verify tests pass
- [x] 4.3 Write failing Vitest for `useEventsFilterState`: `coordinator` URL parameter round-trips and is sent as the `coordinator` query parameter; then implement via `useUrlFilters`; verify tests pass and `npm run build` succeeds

## 5. Integration verification

- [x] 5.1 Add E2E scenarios (via `e2e-tester` agent, `gherkin-bdd` skill) for: coordinator without EVENTS:MANAGE sees, edits and publishes their DRAFT event; coordinator applies a category preset; member filters the list by "Vedoucí"; verify they pass against `./runLocalEnvironment.sh`
- [x] 5.2 Run the full backend and frontend test suites (final run with Modulith optimizations disabled) and `openspec validate extend-coordinator-access-without-manage --strict`; verify all green
- [ ] 5.3 Manual QA on http://localhost:3000 as ZBM9500 made coordinator of a DRAFT event: no 403 requests in the network log when opening the edit form; verify against the spec scenarios
- [ ] 5.4 Add label `BackendCompleted` to GitHub issues #294, #295 and #373

## Workflow follow-up

- Run `npm run refresh-backend-server-resources` before committing frontend changes.
- Code review before each commit, then archive the change (`/openspec-archive-change`) after review.
