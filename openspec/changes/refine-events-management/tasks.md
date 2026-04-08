## 1. Database schema — relax `events.location`

- [ ] 1.1 In `backend/src/main/resources/db/migration/V001__initial_schema.sql`, change `location VARCHAR(200) NOT NULL` on the `events` table to `location VARCHAR(200) NULL`
- [ ] 1.2 Restart the backend locally and confirm Flyway re-applies V001 on the in-memory H2; verify via `INFORMATION_SCHEMA.COLUMNS` that `events.location` is nullable

## 2. Event domain — location optional (TDD)

- [ ] 2.1 Add failing `EventTest`: `Event.create(...)` succeeds when `location` is `null` and the resulting aggregate has `location == null`
- [ ] 2.2 Add failing test: `Event.create(...)` succeeds when `location` is an empty string — at the domain level an empty string is accepted (normalization happens at the API boundary if needed; the domain does not reject it)
- [ ] 2.3 Add failing test: `Event.createFromOris(...)` succeeds when `location` is `null`
- [ ] 2.4 Add failing test: `Event.update(...)` can set an existing event's location to `null`
- [ ] 2.5 Remove `@NotBlank` from `location` in `CreateEvent`, `UpdateEvent`, `CreateEventFromOris`, and `SyncFromOris`; keep `@Size(max = 200)`
- [ ] 2.6 Delete `validateLocation(...)` and its call sites in `Event.create(...)` and `Event.createFromOris(...)`
- [ ] 2.7 Update the aggregate's class-level Javadoc "Business invariants" block so "location" is no longer listed as required
- [ ] 2.8 Verify tests 2.1–2.4 pass

## 3. Event persistence — verify null round-trip (TDD)

- [ ] 3.1 Add failing `EventJdbcRepositoryTest`: save and reload an `Event` with `location == null`; verify the loaded event has `location == null`
- [ ] 3.2 Confirm `EventMemento` already maps `location` as a plain nullable string (no code change expected)
- [ ] 3.3 Verify test 3.1 passes

## 4. Event REST API — accept null location (TDD)

- [ ] 4.1 Add failing `EventControllerTest`: `POST /api/events` with body missing `location` returns 201 and persists the event with `location == null`
- [ ] 4.2 Add failing test: `PUT /api/events/{id}` (or the inline edit endpoint used in the UI) can clear an existing location by submitting `null`
- [ ] 4.3 Add failing `EventControllerNoOrisTest` / `EventManagementE2ETest` assertion: ORIS-import path succeeds when the upstream `EventDetails` has no location
- [ ] 4.4 Implement any normalization at the controller boundary if the existing code rejects `null` or blank (should be unnecessary after task 2 but verify)
- [ ] 4.5 Verify tests 4.1–4.3 pass

## 5. Calendar item description — null-safe join (TDD)

- [ ] 5.1 Add failing `CalendarItemTest` unit test: `buildEventDescription(null, "PBM", null)` returns `"PBM"`
- [ ] 5.2 Add failing test: `buildEventDescription("Senomaty", "PBM", null)` returns `"Senomaty - PBM"` (behavior preserved)
- [ ] 5.3 Add failing test: `buildEventDescription("Senomaty", "PBM", "https://example")` returns `"Senomaty - PBM\nhttps://example"` (behavior preserved)
- [ ] 5.4 Add failing test: `buildEventDescription(null, null, "https://example")` returns `"https://example"` (or the equivalent of "just the URL")
- [ ] 5.5 Add failing test: `buildEventDescription(null, null, null)` returns `null` (not empty string)
- [ ] 5.6 Add failing test: `buildEventDescription("", "   ", null)` returns `null` — blank inputs are treated the same as null
- [ ] 5.7 Rewrite `buildEventDescription(...)` in `CalendarItem.java` as a null-safe join: collect non-blank values from `{location, organizer}`, join with `" - "`, append `"\n" + websiteUrl` when the URL is non-blank, return the result or `null` if the result is empty
- [ ] 5.8 Verify tests 5.1–5.6 pass

## 6. Calendar sync from events — null location path (TDD)

- [ ] 6.1 Add failing `CalendarEventSyncServiceTest`: when `handleEventPublished(...)` runs for an event with `location == null`, the created calendar item has a description that contains the organizer only (per the new `buildEventDescription` rules)
- [ ] 6.2 Add failing test: when `handleEventUpdated(...)` runs and the event has `location == null`, the synced calendar item's description is updated accordingly
- [ ] 6.3 Verify that `CalendarItem.createForEvent(...)` and `synchronizeFromEvent(...)` tolerate a null description produced by `buildEventDescription` (they should, because the calendar-items proposal already made description nullable)
- [ ] 6.4 Verify tests 6.1 and 6.2 pass

## 7. Remove manual finish endpoint (TDD)

- [ ] 7.1 Add failing `EventControllerTest` negative test: `POST /api/events/{id}/finish` returns 404 or 405 (endpoint removed)
- [ ] 7.2 Add failing test: the detail representation for an ACTIVE event does NOT contain a `finishEvent` affordance
- [ ] 7.3 Delete the `@PostMapping("/{id}/finish")` handler in `EventController.java`
- [ ] 7.4 Remove the `finishEvent` affordance line from the ACTIVE branch of `addLinksForEvent`
- [ ] 7.5 Remove `finishEvent(EventId)` from `EventManagementPort` and its implementation in `EventManagementService`
- [ ] 7.6 Delete any existing tests that exercised the manual-finish endpoint or the port method (or convert them to the negative tests from 7.1/7.2)
- [ ] 7.7 Verify `EventCompletionScheduler` and `finishExpiredActiveEvents(...)` still work: re-run the scheduler test and confirm an ACTIVE past-date event still transitions to FINISHED via `Event.finish()`
- [ ] 7.8 Verify `Event.finish()` retains its `status.validateTransition(EventStatus.FINISHED)` guard (no change, just confirm)
- [ ] 7.9 Verify tests 7.1 and 7.2 pass

## 8. Events list — row-level management affordances (refactor + TDD)

- [ ] 8.1 Extract a private helper `addManagementAffordances(WebMvcLinkBuilder selfLink, Event event)` (or equivalent signature) on `EventController` that attaches edit / publish / cancel / sync-from-ORIS affordances according to status + ORIS integration state. The helper intentionally does not touch register/unregister — those stay in the caller because their logic depends on the current user
- [ ] 8.2 Refactor `addLinksForEvent` (detail) to call the new helper instead of switching on status inline
- [ ] 8.3 Refactor `addLinksForListItem` (list) to call the same helper before attaching register/unregister affordances
- [ ] 8.4 Add failing `EventControllerTest`: a DRAFT event row in the list response carries `updateEvent`, `publishEvent`, `cancelEvent` affordances
- [ ] 8.5 Add failing test: an ACTIVE event row in the list response carries `updateEvent`, `cancelEvent` affordances (and NO `finishEvent`)
- [ ] 8.6 Add failing test: an ORIS-imported DRAFT row additionally carries the `syncEventFromOris` affordance
- [ ] 8.7 Add failing test: a non-ORIS DRAFT row does NOT carry the `syncEventFromOris` affordance
- [ ] 8.8 Add failing test: a FINISHED or CANCELLED row carries no management affordances (neither edit, publish, cancel, nor sync)
- [ ] 8.9 Add failing test: a regular member (no EVENTS:MANAGE) sees only register/unregister affordances in the list (no management actions) — rely on the existing `@HasAuthority` / affordance visibility mechanism used elsewhere
- [ ] 8.10 Verify the existing `addLinksForEvent` detail test cases still pass after the refactor (same set of affordances on the detail response)
- [ ] 8.11 Verify tests 8.4–8.9 pass

## 9. Frontend — ORIS import dialog radio buttons

- [ ] 9.1 In `frontend/src/components/events/ImportOrisEventModal.tsx`, change `selectedRegions` state type from `string[]` to `string` and default it to `'JM'`
- [ ] 9.2 Replace the three checkbox inputs with three radio inputs (same name attribute) for `JM`, `M`, `ČR`
- [ ] 9.3 Update the `fetchEvents` caller so it passes a single-element array (or refactor `fetchEvents` to accept a single string and build the URL with one `region` param)
- [ ] 9.4 Update `ImportOrisEventModal.test.tsx` to assert that picking "ČR" clears the "Jihomoravská" selection
- [ ] 9.5 Verify the test passes
- [ ] 9.6 Manual QA: open the dialog in the running app and confirm the three options behave as radio buttons (exactly one always selected) and the events list refreshes on each change

## 10. Frontend — events list management actions

- [ ] 10.1 Locate the events list page component and its row-renderer
- [ ] 10.2 Add an "Akce" column (or extend the existing one) that renders buttons for each HAL-Forms affordance on the row — follow the pattern used by the members table
- [ ] 10.3 The column should render buttons for `updateEvent`, `cancelEvent`, `syncEventFromOris`, and the existing register/unregister affordances, in that order
- [ ] 10.4 Verify via manual QA: as admin (`ZBM9000`), confirm DRAFT rows show Upravit + Publikovat + Zrušit; ACTIVE rows show Upravit + Zrušit + Synchronizovat (if ORIS); FINISHED/CANCELLED rows show no management actions
- [ ] 10.5 As regular member (`ZBM9500`), confirm rows show only register/unregister where applicable and no management actions

## 11. Frontend — remove "Ukončit akci" button

- [ ] 11.1 Locate the event detail page component in `frontend/src`
- [ ] 11.2 Remove the "Ukončit akci" button entirely (both the UI element and the mutation hook it calls)
- [ ] 11.3 Remove any frontend type or client-code reference to the `finishEvent` affordance
- [ ] 11.4 Regenerate `klabisApi.d.ts` via `npm run openapi` after the backend endpoint has been deleted, so stale types are gone

## 12. Frontend — handle missing location in list and detail

- [ ] 12.1 Events list: verify the location cell renders empty (not `null`, not `undefined`) when an event has no location
- [ ] 12.2 Event detail page: verify the location row is hidden entirely (not shown with an empty value) when the event has no location
- [ ] 12.3 Manually test: import an ORIS event known to have no location, open both list and detail, confirm rendering

## 13. Regression / QA walkthrough

- [ ] 13.1 Run the backend test suite via the `test-runner` agent; all green
- [ ] 13.2 Run the frontend test suite via the `test-runner` agent; all green
- [ ] 13.3 Log in as admin, create a manual event without a location → success; detail page hides the location row
- [ ] 13.4 Open the ORIS import dialog; confirm the region picker is a radio group; pick each of the three options and confirm the list reloads
- [ ] 13.5 Import an ORIS event that has no location (or, if none is available in the current ORIS sandbox, mock one in the integration test fixture) → success
- [ ] 13.6 Confirm the calendar shows the newly created event as a calendar item with a description that does not start with a stray `" - "`
- [ ] 13.7 Confirm the event detail page no longer shows "Ukončit akci"
- [ ] 13.8 Verify the scheduled completion path still works: create an ACTIVE event with an event date of yesterday (or tweak the test clock) and confirm the scheduler transitions it to FINISHED
- [ ] 13.9 Verify the events list shows the expected Actions column with context-sensitive buttons for admin and only register/unregister for regular members
