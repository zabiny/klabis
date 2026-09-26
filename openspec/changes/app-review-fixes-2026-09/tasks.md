## 1. Disciplines menu/endpoint authority (EVENTS:MANAGE)

- [x] 1.1 Write/adjust `DisciplineControllerTest` (or equivalent authorization test) asserting `listDisciplines` returns 403 for a user with only `EVENTS_READ` and 200 for `EVENTS_MANAGE` — confirm it fails first (red).
- [x] 1.2 Update `docs/openapi/spec/events.yaml`: `listDisciplines` `x-klabis-authority` from `EVENTS_READ` to `EVENTS_MANAGE`; regenerate OpenAPI bundle + backend/frontend generated sources.
- [x] 1.3 Run the test from 1.1 to green; add/adjust a `DisciplinesRootPostprocessor` test asserting the `disciplines` root link is absent for `EVENTS_READ`-only and present for `EVENTS_MANAGE`.
- [x] 1.4 Fix the misleading comment in `DisciplineController.java` (references `klabisAfford` where `klabisLinkTo` is used).
- [ ] 1.5 Manually verify in the running app (or an E2E/QA pass) that the "Disciplíny" menu entry disappears for a member-only test user and stays for admin.

## 2. Member `active` field authorization in detail response

- [x] 2.1 Write a failing test (e.g. in `MemberControllerTest`) asserting `MemberDetailsResponse.active` is omitted from the JSON for a caller without `MEMBERS_MANAGE`, and present for a caller with it.
- [x] 2.2 Update `docs/openapi/spec/members.yaml`: add `x-klabis-authority: MEMBERS_MANAGE` to `MemberDetailsResponse.active`; regenerate OpenAPI sources.
- [x] 2.3 Run the test from 2.1 to green.
- [x] 2.4 Frontend: verify the "Aktivní"/"Neaktivní" badge on the member detail page (`MemberDetailPage.tsx`) naturally disappears when the field is absent from the response (should require no frontend code change — confirm with a component/integration test or manual check).

## 3. Sync status indicator in the events list + removal of `syncEventFromOris`

- [ ] 3.1 Write a failing test on `EventSummaryPostprocessor`/`EventControllerTest` (`listEvents`) asserting a `sync` link is present for an ORIS-enrolled DRAFT/ACTIVE event row and absent for a non-enrolled one.
- [ ] 3.2 Add the `isEnrolled(eventId)` + `SyncApi.getSyncState` `sync` link block to `EventSummaryPostprocessor`, mirroring `EventDetailsPostprocessor`. Run 3.1 to green.
- [ ] 3.3 Frontend: verify `EventsPage.tsx` renders `SyncStatusIndicator` for the new `sync` link with no code change needed (existing `syncLink && <SyncStatusIndicator .../>` wiring); add/adjust a component test asserting the icon renders when `_links.sync` is present in a row.
- [ ] 3.4 Write a failing test asserting `409 NeedsDecision`-equivalent behavior from `SyncApi.synchronizeNow` when the target sync record is `CONFLICT`/`FAILED` for an event — confirms the generic engine already covers what `EventSyncNeedsResolutionException` guarded, before removing it.
- [ ] 3.5 Remove the `syncEventFromOris` affordance branches (DRAFT/ACTIVE) from `EventAffordanceSupport.addManagementAffordances`; update/remove affected assertions in `EventControllerTest`.
- [ ] 3.6 Remove `OrisEventController.syncEventFromOris` handler, `OrisEventImportPort.syncEventFromOris`, `OrisEventImportService.syncEventFromOris`, and `EventSyncNeedsResolutionException` (if unreferenced after the above); remove/adjust `OrisEventImportServiceTest` and `OrisEventControllerTest` accordingly.
- [ ] 3.7 Remove `POST /api/events/{id}/sync-from-oris` (`syncEventFromOris`) operation and its `x-hal-templates` entry from `docs/openapi/spec/events.yaml`; regenerate OpenAPI sources.
- [ ] 3.8 Full backend build/test run for the `events` module to confirm no dangling references remain.
- [ ] 3.9 Manual/QA verification: events list row for an ORIS-imported event shows the sync status indicator (not a bare "Synchronizovat" button); clicking it opens the overlay and "Synchronizovat teď" triggers a sync pass.

## 4. Sync status indicator for members (list + detail)

- [ ] 4.1 Write a failing test on `MemberSummaryPostprocessor`/`MemberControllerTest` (`listMembers`) asserting a `sync` link is present for an ORIS-enrolled member row and absent for a non-enrolled one.
- [ ] 4.2 Add the `isEnrolled(memberId)` + `SyncApi.getSyncState` `sync` link block to `MemberSummaryPostprocessor`, mirroring `MemberDetailsPostprocessor`. Run 4.1 to green.
- [ ] 4.3 Add `SyncStatusIndicator` (`mode="icon"`) to `MembersPage.tsx`'s row actions, reading `_links.sync` per row, mirroring `EventsPage.tsx`; add/adjust a component test.
- [ ] 4.4 Add `SyncStatusIndicator` (`mode="icon+date"`) to `MemberDetailPage.tsx`, reading `resourceData._links?.sync`, mirroring `EventDetailPage.tsx`; add/adjust a component test.
- [ ] 4.5 Manual/QA verification: an ORIS-linked member shows the sync indicator in both the list and detail page; a manually-registered member shows none.

## 5. Link-based HAL-FORMS options mechanism (`HalFormsOptionsDef`)

- [ ] 5.1 Write failing unit tests for `HalFormsSupport` asserting: (a) an `Inline`-wrapped option renders as `options.inline` exactly as today, (b) a `Remote`-wrapped option renders as `options.link` pointing at the given href.
- [ ] 5.2 Introduce the `HalFormsOptionsDef` sealed interface (`Inline`/`Remote` records) in `com.klabis.common.ui`.
- [ ] 5.3 Change `klabisAffordWithPromptedOptions` signature to `Map<String, HalFormsOptionsDef>`; update `HalFormsInputPayloadMetadata`/`KlabisHalFormsPropertyMetadataWrapper` to switch on the variant and emit `HalFormsOptions.inline(...)` or `HalFormsOptions.remote(link)` accordingly. Run 5.1 to green.
- [ ] 5.4 Migrate every existing caller of `klabisAffordWithPromptedOptions` (e.g. `categoryId` options in `EventController`) to wrap their inline lists as `HalFormsOptionsDef.Inline(...)` — mechanical change, no behavior difference; confirm existing tests for those call sites still pass unchanged.
- [ ] 5.5 Full backend build/test run to confirm the signature migration didn't silently break another call site.

## 6. Discipline options as a link (uses mechanism from #5)

- [ ] 6.1 Write a failing test asserting `createEventType`/`updateEventType` affordances expose `disciplineIds` as `options.link` (pointing at `GET /api/disciplines`) instead of an inline list.
- [ ] 6.2 Update `EventTypeController` to pass `HalFormsOptionsDef.Remote(linkTo(...DisciplinesApi.listDisciplines...))` for `disciplineIds` instead of the inline options list. Run 6.1 to green.
- [ ] 6.3 Frontend: verify the event type form's discipline select still loads and behaves correctly via `useHalFormOptions`'s existing link-options path (no frontend code change expected); add/adjust a component or E2E check.

## 7. Member options as a link (uses mechanism from #5)

- [ ] 7.1 Write a failing test asserting the affordance(s) using `HalFormsMemberId`-style member fields expose their `options.link` pointing at `GET /api/members/options`, populated by the backend.
- [ ] 7.2 Update the relevant controller(s) to pass `HalFormsOptionsDef.Remote(linkTo(...MembersApi.listMemberOptions...))` for those fields. Run 7.1 to green.
- [ ] 7.3 Remove the hardcoded `{link: {href: "/members/options"}}` fallback in `KlabisFieldsFactory.tsx` (`memberIdFieldRenderer`); trust `conf.prop.options` from the backend response.
- [ ] 7.4 Frontend test: `memberIdFieldRenderer` renders correctly using the backend-provided link, with no hardcoded fallback; regression-test the inline-options branch (`conf.prop.options?.inline`) still short-circuits correctly.
- [ ] 7.5 Manual/QA verification: every existing member-picker field (family group parent/child, training group trainer, event coordinator, etc.) still loads and functions identically.

## 8. Final verification

- [ ] 8.1 Full backend test suite (`test-runner` agent) green.
- [ ] 8.2 Full frontend test suite + `npm run build` (`test-runner` agent / `frontend-developer` agent) green.
- [ ] 8.3 Code review (per `CLAUDE.md`: use the appropriate review agent) on the full diff before commit.
- [ ] 8.4 Run `refresh-backend-server-resources` if frontend files changed, per `frontend/CLAUDE.md`.
- [ ] 8.5 Update `openspec/changes/app-review-fixes-2026-09/tasks.md` checkboxes to reflect final state; run `openspec archive` once verified.
