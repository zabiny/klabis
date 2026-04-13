## Why

A review of the events area surfaced four distinct issues that belong together in one proposal:

- **Bug — ORIS import fails for events with no location.** ORIS occasionally returns events with an empty `Location` field; the import path currently rejects them because location is a required field on the `Event` aggregate. Managers then cannot import those events at all.
- **Bug — ORIS import dialog uses checkboxes for a mutually exclusive choice.** The "level of competition" selector (Jihomoravská / Žebříček Morava / ČR) is modelled as checkboxes, but the three values are exclusive — selecting ČR does not clear a previous Jihomoravská selection, leading to confusing combined queries.
- **Dead code — manual "Finish event" action.** Event managers can still click a "Ukončit akci" button that calls a REST endpoint to manually mark an ACTIVE event as FINISHED. The background scheduler already performs this transition automatically once the event date passes, and a manual override adds no value while cluttering the UI and the API surface.
- **UX gap — events list table has no row-level management actions.** Managers have to open each event's detail page just to edit, cancel, or re-sync from ORIS. The members table already has a proper "actions" column driven by HAL links; the events table should follow the same pattern.

## What Changes

- **Event location becomes optional.** The domain, API, database schema, and UI all accept events without a location. Existing ORIS imports that previously failed now succeed.
- **Calendar items generated from events gracefully handle a missing location.** The synchronizer joins the values it actually has (`location`, `organizer`) with `" - "` as a separator, and falls back to the website URL alone when both are missing. If the resulting description would be empty it is stored as `null` rather than an empty string.
- **ORIS import dialog switches from checkboxes to radio buttons.** Frontend-only change; the backend endpoint already tolerates a single-value query parameter.
- **BREAKING — manual finish endpoint is removed.** `POST /api/events/{id}/finish` is deleted along with its port method, service method, HAL-Forms affordance on the detail representation, and UI button. The domain `Event.finish()` method stays because the scheduled `finishExpiredActiveEvents(...)` path continues to use it. The status-transition guard inside `Event.finish()` is preserved so the scheduler cannot accidentally re-finish a closed event.
- **Events list table gains row-level management affordances.** `addLinksForListItem` is refactored to share affordance-building logic with `addLinksForEvent`. Managers see "Upravit", "Synchronizovat" (only for ORIS-imported events in DRAFT/ACTIVE), and "Zrušit" (DRAFT/ACTIVE) directly in the list, alongside the existing register/unregister action. The status column behavior is unchanged.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `events`: location becomes optional on create/update/import, the manual "finish" scenario is removed from the status lifecycle, ACTIVE event actions no longer include "finish", and the events list table gains row-level management actions.
- `calendar-items`: the "Automatic Synchronization from Events" scenario is updated so that event location is treated as optional when building the calendar item description.

## Impact

**Backend:**
- `events/domain/Event.java` — drop `@NotBlank` on `location` in `CreateEvent`, `UpdateEvent`, `CreateEventFromOris`, `SyncFromOris`; delete `validateLocation()` and the call sites in `create(...)` / `createFromOris(...)`. `location` remains a plain `String` field; `null` is a legal value.
- `events/infrastructure/restapi/EventController.java` — delete `@PostMapping("/{id}/finish")` handler, remove the `finishEvent` affordance from `addLinksForEvent`, and refactor `addLinksForListItem` so it shares affordance-building with `addLinksForEvent` via a private helper. The helper adds edit / publish / cancel / sync-from-ORIS affordances to both the list and detail representations according to status, ORIS integration state, and user permissions (managers vs. regular members).
- `events/application/EventManagementPort.java` — remove the `finishEvent(EventId)` method from the primary port.
- `events/application/EventManagementService.java` — remove the `finishEvent(EventId)` implementation. `finishExpiredActiveEvents(LocalDate)` stays — scheduler still calls it. The domain `Event.finish()` method stays with its `status.validateTransition(EventStatus.FINISHED)` guard intact.
- `events/infrastructure/scheduler/EventCompletionScheduler.java` — no change required; it already calls `finishExpiredActiveEvents(...)`, not the removed port method.
- `backend/src/main/resources/db/migration/V001__initial_schema.sql` — change `location VARCHAR(200) NOT NULL` to `location VARCHAR(200) NULL` on the `events` table (in-memory H2, reset on restart, no data migration per project policy).
- `calendar/domain/CalendarItem.java` — update `buildEventDescription(location, organizer, websiteUrl)` so it joins only non-null / non-blank values using `" - "`, appends the website URL on a new line when present, and returns `null` (not an empty string) when nothing can be joined. Update `createForEvent(...)` and `synchronizeFromEvent(...)` callers so a `null` description is allowed (the description field on `CalendarItem` is already nullable per the preceding calendar-items proposal).
- `calendar/application/CalendarEventSyncService.java` — no functional change; its contract becomes "pass through whatever `EventData` provides, including a null location".
- `events/infrastructure/jdbc/EventMemento.java` + `EventRepositoryAdapter.java` — confirm they already round-trip a null location (they should — the location column is a plain string field). No change expected beyond tests.
- Tests — cover ORIS import with a missing location, create/update with missing location, calendar item sync from an event without location, removal of the manual finish endpoint (negative test: endpoint returns 404/405), and HAL-Forms affordance presence on list items (edit / cancel / sync show up exactly when they should).

**Frontend:**
- `frontend/src/components/events/ImportOrisEventModal.tsx` — change the region picker from multi-select checkboxes to single-select radio buttons. State type moves from `string[]` to `string`. The request stays `?region=<value>` with a single value, so the backend `GET /api/oris/events` endpoint needs no change (it still accepts a `List<String>` that happens to contain one item).
- Events list page — new "Akce" column rendering buttons from the HAL-Forms affordances on each row (edit, synchronizovat, zrušit, plus the existing register/unregister). Same HAL-driven pattern as the members table.
- Event detail page — remove the "Ukončit akci" button. The edit / cancel / sync-from-ORIS buttons stay.
- Event list/detail pages — make sure a row or detail without a location renders as an empty cell / hidden row rather than a literal `null`.

**Specs:**
- `openspec/specs/events/spec.md` — delta updates to `Create Event` (location moves to optional), `Events Table Display` (location cell empty when missing; new "row-level management actions" scenarios), `Event Detail Page` (location row hidden when missing), `Event Status Lifecycle` (remove the manual-finish scenario), `Get Event Detail` (remove "finish" from ACTIVE event actions), and a new scenario for ORIS import succeeding with no location.
- `openspec/specs/calendar-items/spec.md` — delta update to `Automatic Synchronization from Events` so the event→calendar-item description explicitly treats location as optional.

**Out of scope:**
- Changing `OrisController.listOrisEvents` to take a scalar `region` param. Backend stays multi-value tolerant; frontend sends exactly one value.
- Any rework of the background scheduled completion path or the automatic calendar sync flow beyond the null-location handling already noted.
- Changes to the status column's field-level security.
