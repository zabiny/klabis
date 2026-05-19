# Team Coordination File — calendar "Můj rozvrh" filter

Tento soubor je sdílený mezi všemi subagenty na proposalu `calendar-my-schedule-filter`. Každý subagent ho **musí** přečíst před začátkem a **musí** sem připsat krátké shrnutí toho, co udělal nebo na co narazil.

## Proposal summary

Add an optional `mySchedule=true` query parameter to `GET /api/calendar`. When ON, return only event-date calendar items linked to events where the current user is either an active participant or the event coordinator. Frontend gets a toggle in calendar header, state in URL only.

Key constraints from `design.md`:
- Two `EventFilter` queries unioned in calendar use case (NOT one combined query — that would be AND semantics).
- Item-type filtering happens in calendar module (linkedEventId IN set AND itemType=event-date).
- Plain `@RequestParam`, no HAL-Forms affordance.
- Default OFF, URL only.
- HAL `self`/`prev`/`next` links must preserve `mySchedule` param.

## Iteration plan

Vertical slices, app musí být funkční po každé iteraci:

1. **Backend slice — participant match + coordinator union + HAL links**: tasks 1, 2, 5 from tasks.md (test-first; controller param + use-case branch + HAL link preservation). Tasks 3 & 4 are mostly verification of existing behaviour — bundled.
2. **Frontend slice — toggle + API call + URL preservation + localisation**: tasks 6–10.
3. **Cross-cutting verification + bundle refresh**: tasks 11, 12.

## Progress log

### 2026-05-19 — Backend slice (tasks 1–5) — DONE

All backend tasks shipped and green (2531/2533; 2 pre-existing failures unrelated).

**What shipped:**
- New cross-module secondary port `EventScheduleQuery` in `com.klabis.events` root — two methods: `findEventIdsByRegistration` + `findEventIdsByCoordinator`, each taking `MemberId` + date range. Implementation in `com.klabis.events.infrastructure.jdbc.EventScheduleQueryImpl` (package-private) using existing `EventFilter` internally.
- `CalendarManagementPort.listCalendarItems` extended with `@Nullable MemberId myScheduleMemberId` (4th arg). When non-null, service runs both queries, unions IDs, and keeps only `EVENT_DATE` items whose `linkedEventId` is in the union.
- `CalendarController`: added optional `Boolean mySchedule` `@RequestParam` + `@ActingUser CurrentUserData currentUser`. Parameter threaded through self link and `prev`/`next` navigation links.
- `CalendarEventSyncIntegrationTest`: added `@MockitoBean EventScheduleQuery` — required because STANDALONE context cannot load the events-module adapter.

**Surprises:**
- Spring HATEOAS renders optional `@RequestParam` as `{&mySchedule}` in URI templates even when null. Test checking `not(containsString("mySchedule"))` would always fail — fixed by asserting `not(containsString("mySchedule=true"))` instead.
- `EventFilter` lives in `com.klabis.events.domain` (internal), so it cannot be referenced from calendar. The `EventScheduleQuery` port encapsulates both filter constructions entirely inside the events module.

---

### 2026-05-19 — Fix: no-member-profile + mySchedule

When `mySchedule=true` and the authenticated user has no member profile (`currentUser.memberId() == null`), the controller now short-circuits before calling the service and returns an empty `CollectionModel` (200, no items) with self/prev/next links preserved — `resolveMyScheduleMemberId` returning `null` was silently disabling the filter and returning the full unfiltered calendar.

---

### 2026-05-19 — Frontend slice (tasks 6–10) — DONE

All frontend tests green (1469/1469).

**Files changed:**
- `frontend/src/pages/calendar/CalendarPage.tsx` — toggle in header wired to URL search params; toggle drives fetch (`mySchedule=true` appended/removed); month navigation preserves param via backend `prev`/`next` HAL links.
- `frontend/src/pages/calendar/CalendarPage.test.tsx` — tests for toggle default state, URL append/remove, deep link rendering, month navigation, empty-grid + active filter visibility.
- `frontend/src/localization/labels.ts` — added "Můj rozvrh" label, used in toggle + active-filter indicator.

No banner / empty-state copy introduced (per design). Toggle visible at all times.

---
