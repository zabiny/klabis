# Tasks

Organized as **vertical end-to-end slices**, each cutting through domain → persistence → API → frontend → tests and independently committable/testable (per project convention: prefer vertical slices over horizontal ones).

Red-Green-Refactor throughout: write the failing test, make it pass with the minimal version, refactor. Coverage target: >80% overall, 100% for domain logic (per `openspec/config.yaml`).

Ordering note: **slice 2 must land in the same release as slice 1** — D4 removes the API mechanism that today hides the status column from non-managers, so the explicit frontend gate has to ship with it (design D5, risk "The frontend's implicit column hiding stops working").

Test entry points: `EventFilterE2ETest` for list-visibility combinations end-to-end, `EventJdbcRepositoryTest` for the adapter's raw-SQL dimension, `EventControllerTest` for field security, `EventsPage.test.tsx` / `EventDetailPage.test.tsx` for the UI.

## 1. Slice: list visibility — cancelled events only for managers and registered members

- [x] 1.1 Failing test first (`EventFilterE2ETest`): a regular member (no `EVENTS:MANAGE`) gets an event list containing a DRAFT, an ACTIVE, a FINISHED and a CANCELLED event they are registered for, plus a CANCELLED event they are **not** registered for — assert the response contains the DRAFT-free set and the registered CANCELLED event, and **not** the unregistered CANCELLED one.
- [x] 1.2 `EventFilter`: add the `cancelledVisibleTo` component (nullable `MemberId`) and `withCancelledVisibleTo(MemberId)`, matching the existing `with*` copy-factory style; thread the new component through every existing factory and `with*` method (all pass it as null).
- [x] 1.3 `EventManagementPort.listEvents` + `EventManagementService.listEvents`: add the `MemberId viewerMemberId` parameter (nullable, ignored for managers) and implement the non-manager policy exactly as in design D1 — including the `requestsOnlyStatus(CANCELLED)` guard for viewers without a member profile (the `withExcludedStatus` empty-set collapse that would otherwise expose every event).
- [x] 1.4 `EventRepositoryAdapter`: add `findIdsByCancelledAndRegistered(MemberId)` (SQL: `status = 'CANCELLED' AND EXISTS registration for the member`) and wire the OR-condition `(status <> 'CANCELLED') OR (id IN …)` into `buildNonFulltextConditions`; skip both the pre-fetch and the condition when the filter already excludes CANCELLED, and fall back to the status-only condition when the id list is empty (no `IN ()`).
- [x] 1.5 `EventController.listEvents`: pass `currentUser.memberId()` (or null) alongside the existing `EVENTS:MANAGE` check.
- [x] 1.6 Failing tests (`EventFilterE2ETest`) for the guard and the combination: regular member **without** a member profile filtering `status=CANCELLED` → empty page; regular member with `status=CANCELLED` → only their own cancelled events; member registered for none → empty; `"Moje přihlášky"` filter still returns the member's cancelled events; fulltext + cancelled visibility combine with AND.
- [x] 1.7 Run backend tests for the events module, refactor if anything smells, commit.

## 2. Slice: ungate `status` on the summary + explicit status-column gate in the table

- [ ] 2.1 `docs/openapi/spec/events.yaml`: remove `x-klabis-authority: EVENTS_MANAGE` from `EventSummaryDto.status`; then `./gradlew openapiBundle` (from `backend/`) and `npm run openapi` (from `frontend/`) to regenerate the bundle and the frontend types. No new fields.
- [ ] 2.2 Failing test first (`EventControllerTest`): `GET /api/events` as a member without `EVENTS:MANAGE` returns `status` for each row (previously masked); the `EVENTS:MANAGE` case keeps returning it; assert the existing `_links`/`_templates` parity of the list is unchanged. Then make it pass.
- [ ] 2.3 Failing test first (`EventsPage.test.tsx`): the status column is **not** rendered for a user whose list response has no `_templates.createEvent`, and **is** rendered when that template is present — both cases include `status` in the row payload (so the test fails without the explicit gate, proving the implicit `hideEmptyColumns` route is gone).
- [ ] 2.4 `EventsPage.tsx`: gate the status column explicitly — `const canManageEvents = Boolean(resourceData?._templates?.createEvent);` and `hidden={!canManageEvents}` on the status `TableCell` (design D5). Keep `hideEmptyColumns={true}` for the other optional columns.
- [ ] 2.5 Run backend + frontend tests, commit. (Ship together with slice 1's release — see ordering note.)

## 3. Slice: struck-through name in the list

- [ ] 3.1 Failing test first (`EventsPage.test.tsx`): a row with `status: 'CANCELLED'` renders its name with a strikethrough; `ACTIVE` / `FINISHED` / `DRAFT` rows render the plain name; the row's action buttons remain present and clickable in the cancelled case.
- [ ] 3.2 `EventsPage.tsx`: add a `dataRender` to the `name` `TableCell` — `status === 'CANCELLED'` → `<span className="line-through …">`, otherwise the plain value; column stays sortable.
- [ ] 3.3 Failing test first (`EventsPage.test.tsx`): the status cell no longer carries a `title` attribute with the cancellation reason (the tooltip is removed per the delta spec).
- [ ] 3.4 `EventsPage.tsx`: remove the status-cell tooltip branch (currently `if (event.status === 'CANCELLED' && event.cancellationReason) return <span title={…}>`).
- [ ] 3.5 Update the existing `EventsPage.test.tsx` expectations that assert the tooltip; run frontend tests, commit.

## 4. Slice: detail page — struck name, "Zrušeno" badge, reason under the name

- [ ] 4.1 Failing test first (`EventDetailPage.test.tsx`): for a cancelled event with a reason — the name is struck through, the "Zrušeno" badge is present, and the reason text is rendered **within the header block, under the name**; for a cancelled event without a reason — struck name and badge present, no reason text.
- [ ] 4.2 `EventDetailPage.tsx`: add `line-through` to the `<h1>` when `event.status === 'CANCELLED'`; render `event.cancellationReason` directly under the name in the header block (error-coloured, small text) when present. Reuse the existing badge (`STATUS_VARIANT.CANCELLED = 'error'` + `getEnumLabel('eventStatus', …)` → "Zrušeno") — no new badge markup.
- [ ] 4.3 Failing test first (`EventDetailPage.test.tsx`): the bottom "AKCE BYLA ZRUŠENA" banner is gone — no element with the `sections.eventCancelled` text; the reason appears exactly once, under the name.
- [ ] 4.4 `EventDetailPage.tsx`: delete the bottom cancellation `Card` (currently rendered for `status === 'CANCELLED'`) and remove the now-unused `sections.eventCancelled` label from `frontend/src/localization/labels.ts`; update the tests that assert the banner (4 of them).
- [ ] 4.5 Run frontend tests, commit.

## 5. Close-out: spec sync, full test run, review

- [ ] 5.1 Verify the delta spec is the implemented behaviour: re-read `openspec/changes/gh-66-event-cancellation-reason/specs/events/spec.md` against the code for all five MODIFIED requirements (including the two dropped scenarios: the list tooltip scenario and "Status column hidden when not returned by API").
- [ ] 5.2 Run the full backend test suite (Modulith verification + architecture tests included) and the full frontend suite; refactor anything the new code duplicated (e.g. if the `cancelled`-aware name renderer ends up needed in more than one place, extract it).
- [ ] 5.3 Manual verification on `http://localhost:3000` as `admin` and as the club member (`ZBM9500`):
  - cancel an ACTIVE event **with** a reason → struck name in the list, badge + reason under the name on the detail; the reason is no longer a list tooltip
  - the registered member still sees that cancelled event in their list (struck name), and can open the detail
  - a member **not** registered for a second cancelled event does not see it in the list
  - the status column is visible for `admin` and absent for the plain member (explicit gate, not the payload)
  - the `"Moje přihlášky"` filter still lists cancelled events with an existing registration
- [ ] 5.4 Code review, then commit; when merged, sync the delta into `openspec/specs/events/spec.md` (`openspec-sync-specs`) and archive the change.

## Out of scope (unchanged from the proposal)

- E-mail/notification of registered members on cancellation (TODO / #28) — the reason is stored so a future template can use it.
- Editing the cancellation reason after cancellation (cancelled events stay immutable).
- Calendar items on cancellation (still deleted; `CalendarEventSyncService.handleEventCancelled`).
