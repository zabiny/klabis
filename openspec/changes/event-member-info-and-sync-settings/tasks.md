# Tasks

## 1. Success handling survives removal of the viewed resource (#417)

- [x] 1.1 Grep the `useAuthorizedMutation` callers and confirm none reads refreshed query data inside its per-call `onSuccess`. Record the result in the PR description; if one does, stop and raise it before continuing.
- [x] 1.2 Rewrite the seam tests in `useAuthorizedFetch.test.ts` first (red): per-call `onSuccess` runs before the refetched data arrives; invalidation is triggered exactly once per successful mutation; no invalidation on a failed write; a failing refetch does not fail the mutation. Verify they fail against the current hook.
- [x] 1.3 Change `useAuthorizedMutation` to start `invalidateAuthorizedQueries` without awaiting it (design D3) and update the hook's JSDoc. Verify with the 1.2 tests and the existing `HalFormDisplay` / `useTemplateDialog` tests (test-runner).
- [x] 1.4 Add a `GroupDetailPage` test where the post-mutation refetch of the group fails with 404 (delete) and 403 (leave, remove oneself as owner), and assert the navigation to `/groups` still happens. Verify with test-runner.
- [x] 1.5 Manually verify in the browser on http://localhost:3000: `ZBM9000` deletes a group and lands on `/groups` with a toast; `ZBM9500` leaves a group and lands on `/groups` without a 403; unregistering from an event closes the dialog with a toast.
- [x] 1.6 Run the two E2E scenarios in `e2e/features/event-registrations.feature` (unregister) against the local environment and verify they pass. If they still fail, report the cause; do not patch the scenarios.

## 2. Registration without categories (#365)

- [x] 2.1 Add failing tests for `EventRegistrationDialog` / `registrationFieldsFactory`: no category field when the `categoryId` options are empty; `categoryId` is not in the submitted payload then; the `*` marker follows `prop.required`. Verify they fail first.
- [x] 2.2 Implement design D4 (hide the field when options are empty, strip `categoryId` alongside the other hidden fields, conditional marker). Verify with the 2.1 tests and the existing dialog tests (test-runner).
- [x] 2.3 Manually verify on http://localhost:3000 that a member can register for an event without categories and that an event with categories still requires one.

## 3. Informace pro členy on an event (#75)

- [x] 3.1 Add `memberInfo` to `docs/openapi/spec/events.yaml` (`EventDto`, `CreateEventRequest`, `UpdateEventRequest`) per the design's REST API table, following the `klabis-api-spec` skill. Regenerate the bundle and frontend types (`npm run openapi`) and verify the backend compiles against the generated interfaces.
- [x] 3.2 Domain, test first: `Event.create` and `Event.update` carry `memberInfo` (blank stored as absent; update refused for FINISHED/CANCELLED like other fields); `syncFromOris` leaves it unchanged; the `OrisEventProjection` of an event is unaffected by it. Verify the domain tests pass (test-runner, events module).
- [x] 3.3 Persistence: add `member_info VARCHAR(2000) NULL` to `events.events` in V001 (with a column comment) and map it in `EventMemento`. Verify with the event repository integration test round-tripping the value.
- [x] 3.4 REST: create converter, `UpdateEventRequestMapper` overlay (absent keeps, null clears, value sets), and detail DTO mapping. Cover with `@WebMvcTest`: a coordinator without EVENTS:MANAGE can change it; a 2001-character value is rejected with a field error; the detail returns it. Verify with test-runner (events module).
- [x] 3.5 Frontend: show the "Informace pro členy" section on `EventDetailPage` with line breaks kept and hidden when empty; include the textarea in the event create/edit form and inline edit; add labels to `labels.ts`. Cover with component tests and verify with test-runner.
- [x] 3.6 Manually verify on http://localhost:3000: a coordinator writes multi-line information, a regular member reads it on the detail page, and clearing it removes the section.
- [x] 3.7 Add the `BackendCompleted` label to issue #75.

## 4. Synchronisation tabs and club key settings (#367)

- [ ] 4.1 `useRootNavigation`: skip the non-menu rel `orisClubKey`, with a unit test asserting it never becomes a menu item. Verify with test-runner.
- [ ] 4.2 `SyncOverviewPage` tabs, test first: order "Vyžaduje pozornost" (default, no `tab`), "Všechny pokusy" (`tab=attempts`), "Nastavení synchronizace" (`tab=settings`); each tab only when its source exists; no tab bar with a single tab; an unknown `tab` falls back to the default; attempts filters keep working under `tab=attempts`. Update the existing `SyncOverviewPage.test.tsx` cases that assume attempts are the default. Verify with test-runner.
- [ ] 4.3 "Klíč klubu ORIS" section in the settings tab: reads `orisClubKey` from the root query, shows held / not held, and offers `setClubKey` / `clearClubKey` through `useTemplateDialog` without leaving the tab; labels in `labels.ts`. Cover with component tests (state rendering, actions shown only when templates exist) and verify with test-runner.
- [ ] 4.4 Update the E2E feature `application-navigation.feature` only if a scenario lists the club key or relies on attempts being the default tab, and verify the navigation scenarios pass.
- [ ] 4.5 Manually verify on http://localhost:3000 as an admin with the `oris` profile: the club key is not in the menu, the synchronisation page opens on "Vyžaduje pozornost", and supplying or discarding the key in "Nastavení synchronizace" updates the state in place.

## 5. Integration checks

- [ ] 5.1 Run `npm run build` and `npm run lint` in `frontend/` and verify both succeed.
- [ ] 5.2 Run the full backend and frontend test suites through the test-runner (final verification, with the modulith skip optimisations disabled) and verify they are green apart from the known pre-existing failures.
- [ ] 5.3 Run `npm run refresh-backend-server-resources` and verify the backend static resources are refreshed.
- [ ] 5.4 Run `openspec validate event-member-info-and-sync-settings --strict` and verify it passes.

## Workflow follow-up

- Code review before committing, per project rules.
- Update proposal `resolve-group-ownership-on-suspension` (#398): adopt #418 into scope and replace design D4's "the debt dialog continues to open separately" with a single dialog listing all blocking reasons.
- Archive the change after review and merge.
