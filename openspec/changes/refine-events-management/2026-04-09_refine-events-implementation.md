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
