# TCF — refine-events-management implementation

**Created:** 2026-04-09
**Proposal:** `refine-events-management`
**Team leader:** main Claude instance

## Purpose

Coordination file for subagents implementing the `refine-events-management` OpenSpec proposal. Every subagent invoked for this proposal MUST read this file first, then append a concise summary of what it did (and any unresolved issues) at the bottom so subsequent subagents can continue where the previous one ended.

## Proposal summary

Four related refinements to the events capability:

1. **Event location becomes optional** end-to-end (domain → DB → REST API → calendar sync → frontend rendering). `buildEventDescription` rewritten to null-safe join.
2. **Manual `POST /api/events/{id}/finish` endpoint removed** (both backend and frontend). Scheduler path via `Event.finish()` stays intact.
3. **Events list gains row-level management actions** — `addLinksForListItem` refactored to share affordance logic with `addLinksForEvent` via a private helper.
4. **ORIS import dialog region picker** switches from checkboxes to radio buttons (frontend only — backend stays `List<String>`).

## Key references

- Proposal: `openspec/changes/refine-events-management/proposal.md`
- Design decisions: `openspec/changes/refine-events-management/design.md`
- Tasks: `openspec/changes/refine-events-management/tasks.md`
- Event delta spec: `openspec/changes/refine-events-management/specs/events/spec.md`
- Calendar-items delta spec: `openspec/changes/refine-events-management/specs/calendar-items/spec.md`

## Iteration plan

Vertical slices; application functional after each iteration.

### Iteration 1 — Location optional end-to-end (backend)
Tasks 1, 2, 3, 4, 5, 6. One delegated backend task: TDD-style, follows tasks.md ordering. At end: manual + ORIS event creation without location works, calendar sync handles null location via null-safe `buildEventDescription`, all backend tests green.

### Iteration 2 — Remove manual finish endpoint (backend)
Task 7. Delete endpoint, port method, service method, affordance. Keep `Event.finish()` + scheduler path. Negative tests for endpoint removal.

### Iteration 3 — Events list row-level affordances (backend)
Task 8. Refactor `addLinksForEvent` / `addLinksForListItem` to share a helper. New tests for affordance set on list rows.

### Iteration 4 — Frontend changes
Tasks 9, 10, 11, 12. ORIS dialog radio buttons, events list Actions column, remove "Ukončit akci" button, null-location rendering. Regenerate `klabisApi.d.ts` after backend endpoint removal.

### Iteration 5 — Final polish
- Run `simplify` skill on all changes from iterations 1–4
- Run code-review agent on the full diff, apply high+ priority findings
- Run full test suite (backend + frontend)
- Final commit

## Commit convention

Conventional Commits. Suggested scope: `events`. Examples:
- `feat(events): allow events without a location`
- `refactor(events): remove manual finish endpoint`
- `refactor(events): share row-level affordance logic between list and detail`
- `feat(frontend): events list row-level management actions`

## Current status

**Iteration:** not started
**Last updated by:** team leader (setup only)

---

## Subagent log (append below)

### 2026-04-09 — Iteration 1 (tasks 1–6) — location optional backend

Implemented the full location-optional change end-to-end in the backend following TDD (RED → GREEN). All 2101 backend tests pass green.

**Changes made:**
- `V001__initial_schema.sql` — `events.location` changed from `NOT NULL` to `NULL`
- `Event.java` — removed `validateLocation(...)` and all three call sites (`create`, `createFromOris`, `update`); removed `@NotBlank` from `location` on `CreateEvent`, `UpdateEvent`; updated Javadoc invariants block; `CreateEventFromOris` and `SyncFromOris` had no `@NotBlank` annotation (only the method call was the gate)
- `EventCreatedEvent.java` + `EventUpdatedEvent.java` — removed `requireNonNull` on location (discovered during test run, location was re-validated in the event constructors)
- `CalendarItem.java` — rewrote `buildEventDescription` as null-safe join (package-visible for testing); removed `validateLocation` from `createForEvent` and `synchronizeFromEvent`; added `ArrayList`/`List` imports
- Tests added: `EventTest` (create null, create empty, createFromOris null, update null), `EventJdbcRepositoryTest` (null location round-trip), `EventControllerTest` (POST without location → 201, PATCH with null location → 204), `EventManagementE2ETest` (full create+get without location), `CalendarItemTest` (6 `buildEventDescription` cases + 2 synchronizeFromEvent null/blank location cases), `CalendarEventSyncServiceTest` (3 null-location sync cases)
- Pre-existing tests that assumed location was required were replaced: `shouldFailWhenLocationIsNull` / `shouldFailWhenLocationIsBlank` in `EventTest.CreateMethod` replaced by positive tests; `shouldFailToUpdateWithNullLocation` / `shouldFailToUpdateWithBlankLocation` in `EventTest.UpdateMethod` replaced by `shouldUpdateEventWithNullLocation`; `shouldFailSynchronizationWhenLocationIsBlank` in `CalendarItemTest` replaced by positive null/blank location sync tests

**Follow-up notes:**
- Task 4.3 — no dedicated ORIS-import controller test was added for a missing-location `EventDetails` fixture; the `EventManagementE2ETest.shouldCreateEventWithoutLocation` covers the manual create path end-to-end and the domain path covers ORIS. A dedicated ORIS fixture test can be added in a later iteration if desired.
- `EventSummaryDto` (list response) — `location` field is a plain nullable `String` with no `@JsonInclude(NON_NULL)`, so it serializes as `"location": null` when absent. Frontend null-location rendering (task 12) will need to handle this explicitly.

### 2026-04-09 — Iteration 2 (task 7) — remove manual finish endpoint

All 2100 backend tests pass green.

**Changes made:**
- `EventManagementPort.java` — removed `finishEvent(EventId)` method
- `EventManagementService.java` — removed `finishEvent(EventId)` implementation
- `EventController.java` — deleted `@PostMapping("/{id}/finish")` handler; removed `finishEvent` affordance from ACTIVE branch of `addLinksForEvent`
- `EventControllerTest.java` — converted `FinishEventTests.shouldFinishEvent` to a negative test asserting 404/405; added second test asserting ACTIVE event detail has no `finishEvent` affordance
- `EventManagementServiceTest.java` — deleted `FinishEventMethod` nested class (both `shouldFinishActiveEvent` and `shouldThrowExceptionWhenEventNotFound`)
- `EventManagementE2ETest.java` — imported `EventManagementPort`; replaced `POST /api/events/{id}/finish` HTTP call in lifecycle test and update-rejection test with `eventManagementPort.finishExpiredActiveEvents(LocalDate.now())` using a past event date; updated affordance assertion to `doesNotExist()` for `finishEvent`

**Invariants confirmed:**
- `Event.finish()` retained with `status.validateTransition(EventStatus.FINISHED)` guard and `EventFinishedEvent` publication (no change)
- `finishExpiredActiveEvents(...)` retained on port and service; `EventCompletionSchedulerTest` (2 tests) passes

**Follow-ups:** none

### 2026-04-09 — Iteration 4 (tasks 9–12) — frontend changes

All 1109 frontend tests pass green (1108 pre-existing + 1 new radio-button test, minus 2 removed `finishEvent` tests = net +1 new test added in `ImportOrisEventModal.test.tsx`, pre-existing `finishEvent` button tests converted/removed in `EventDetailPage.test.tsx` and `labels.test.ts`).

**Changes made:**

- `frontend/src/components/events/ImportOrisEventModal.tsx` — changed `selectedRegions` state from `string[]` to `string` (default `'JM'`); replaced three checkbox inputs with three radio inputs sharing `name="orisRegion"`; renamed `fetchEvents(regions: string[])` → `fetchEvents(region: string)` building a single `?region=` param; replaced `handleRegionToggle` with `handleRegionChange`
- `frontend/src/components/events/ImportOrisEventModal.test.tsx` — added `describe('region picker')` group with 3 tests: radio buttons render, JM selected by default, picking ČR clears Jihomoravská
- `frontend/src/pages/events/EventsPage.tsx` — extended `renderActionsCell` to render icon buttons for `updateEvent` (Pencil), `publishEvent` (Globe), `cancelEvent` (XCircle), `syncEventFromOris` (RefreshCw) before the existing register/unregister buttons; added `Globe`, `Pencil`, `RefreshCw`, `XCircle` to lucide imports; changed `location` type in `EventListData` from `string` to `string | null`; added `dataRender` to the location `TableCell` to handle explicit `null` (renders empty instead of `"null"`)
- `frontend/src/pages/events/EventDetailPage.tsx` — removed `HalFormButton name="finishEvent"` line; removed `CheckCircle` from lucide imports; updated `location` type in `EventDetail` interface to `string | null`
- `frontend/src/pages/events/EventDetailPage.test.tsx` — converted `shows finishEvent button when template exists` test to a negative assertion that the button is NOT rendered even when the template is present
- `frontend/src/localization/labels.ts` — removed `finishEvent` from `templates` and `dialogTitles`
- `frontend/src/localization/labels.test.ts` — removed `finishEvent` assertion from `has template labels` test
- `docs/openapi/klabis-full.json` — removed `/api/events/{id}/finish` path entry
- `frontend/src/api/klabisApi.d.ts` — regenerated via `npm run openapi` (reads `../docs/openapi/klabis-full.json`); `finishEvent` operation and path are gone

**OpenAPI regeneration command:** `npm run openapi` (in `frontend/`), which runs `npx openapi-typescript ../docs/openapi/klabis-full.json -o ./src/api/klabisApi.d.ts`. The `klabis-full.json` was updated manually first (backend not running) by removing the `/api/events/{id}/finish` path block. The backend source already had the endpoint deleted since iteration 2.

**Manual QA notes (pending — requires running backend):**
- Tasks 9.6, 10.4, 10.5, 12.3 are manual QA steps; backend was not running during this iteration. These are carried forward to iteration 5's QA walkthrough.
- Detail page: `location` row condition `(isEditing || event.location)` correctly evaluates to `false` for both `undefined` and explicit JSON `null`, so the row hides with no code change beyond the type annotation.
- List page: `dataRender` on location cell returns `null` (not rendered) when value is falsy, avoiding the default `String(value)` which would render `"null"`.

**Follow-ups:** None blocking. Manual QA deferred to iteration 5.

### 2026-04-09 — Iteration 3 (task 8) — row-level list affordances refactor

All 2107 backend tests pass green (2100 pre-existing + 7 new).

**Helper signature chosen:**
```java
private Link addManagementAffordances(Link selfLink, Event event)
```
Added `import org.springframework.hateoas.Link` to support the simple type name.

**Changes made:**
- `EventController.java` — extracted `addManagementAffordances(Link, Event)` private helper containing the status-based switch (DRAFT → update+publish+cancel, ACTIVE → update+cancel, FINISHED/CANCELLED → nothing) plus the conditional `syncEventFromOris` affordance (when `orisIntegrationActive && orisId != null && status in {DRAFT, ACTIVE}`)
- `addLinksForEvent` (detail) — replaced the inline switch with a call to the helper; kept the ACTIVE registration open/closed block inline since it depends on `currentUser`
- `addLinksForListItem` (list) — calls `addManagementAffordances` first, then conditionally appends register/unregister; register logic is guarded only by `event.areRegistrationsOpen()` (no status guard needed since that method already returns false for FINISHED/CANCELLED)

**Tests written by previous agent (7):** all passed without modification — they were correct as written.

**Permission filtering (task 8.9):** worked automatically via `klabisAfford()` / `@HasAuthority(Authority.EVENTS_MANAGE)` on `updateEvent`, `publishEvent`, `cancelEvent`, `syncEventFromOris`. A user with only `EVENTS_READ` sees the register affordance but none of the management affordances, exactly as test 8.9 asserts.

**Detail page (task 8.10):** all pre-existing `addLinksForEvent` tests (`GetEventTests`, `FinishEventTests`, affordance assertions) pass unchanged.

**Follow-ups:** none

### 2026-04-09 — Iteration 4 (tasks 9–12) — frontend changes

All 1109 frontend tests pass green (1108 pre-existing + 1 new radio-button test, minus 2 removed `finishEvent` tests = net +1 new test added in `ImportOrisEventModal.test.tsx`, pre-existing `finishEvent` button tests converted/removed in `EventDetailPage.test.tsx` and `labels.test.ts`).

**Changes made:**

- `frontend/src/components/events/ImportOrisEventModal.tsx` — changed `selectedRegions` state from `string[]` to `string` (default `'JM'`); replaced three checkbox inputs with three radio inputs sharing `name="orisRegion"`; renamed `fetchEvents(regions: string[])` → `fetchEvents(region: string)` building a single `?region=` param; replaced `handleRegionToggle` with `handleRegionChange`
- `frontend/src/components/events/ImportOrisEventModal.test.tsx` — added `describe('region picker')` group with 3 tests: radio buttons render, JM selected by default, picking ČR clears Jihomoravská
- `frontend/src/pages/events/EventsPage.tsx` — extended `renderActionsCell` to render icon buttons for `updateEvent` (Pencil), `publishEvent` (Globe), `cancelEvent` (XCircle), `syncEventFromOris` (RefreshCw) before the existing register/unregister buttons; added `Globe`, `Pencil`, `RefreshCw`, `XCircle` to lucide imports; changed `location` type in `EventListData` from `string` to `string | null`; added `dataRender` to the location `TableCell` to handle explicit `null` (renders empty instead of `"null"`)
- `frontend/src/pages/events/EventDetailPage.tsx` — removed `HalFormButton name="finishEvent"` line; removed `CheckCircle` from lucide imports; updated `location` type in `EventDetail` interface to `string | null`
- `frontend/src/pages/events/EventDetailPage.test.tsx` — converted `shows finishEvent button when template exists` test to a negative assertion that the button is NOT rendered even when the template is present
- `frontend/src/localization/labels.ts` — removed `finishEvent` from `templates` and `dialogTitles`
- `frontend/src/localization/labels.test.ts` — removed `finishEvent` assertion from `has template labels` test
- `docs/openapi/klabis-full.json` — removed `/api/events/{id}/finish` path entry
- `frontend/src/api/klabisApi.d.ts` — regenerated via `npm run openapi` (reads `../docs/openapi/klabis-full.json`); `finishEvent` operation and path are gone

**OpenAPI regeneration command:** `npm run openapi` (in `frontend/`), which runs `npx openapi-typescript ../docs/openapi/klabis-full.json -o ./src/api/klabisApi.d.ts`. The `klabis-full.json` was updated manually first (backend not running) by removing the `/api/events/{id}/finish` path block. The backend source already had the endpoint deleted since iteration 2.

**Manual QA notes (pending — requires running backend):**
- Tasks 9.6, 10.4, 10.5, 12.3 are manual QA steps; backend was not running during this iteration. These are carried forward to iteration 5's QA walkthrough.
- Detail page: `location` row condition `(isEditing || event.location)` correctly evaluates to `false` for both `undefined` and explicit JSON `null`, so the row hides with no code change beyond the type annotation.
- List page: `dataRender` on location cell returns `null` (not rendered) when value is falsy, avoiding the default `String(value)` which would render `"null"`.

**Follow-ups:** None blocking. Manual QA deferred to iteration 5.
