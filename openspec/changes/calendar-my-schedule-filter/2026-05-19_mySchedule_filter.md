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

### 2026-05-20 — Backend simplification (three changes in one pass)

**Change 1 — `EventScheduleQuery` collapsed to single SQL method.**
`findEventIdsByRegistration` + `findEventIdsByCoordinator` (two separate queries that hydrated full `Event` aggregates) replaced by one `findEventIdsForMemberSchedule(MemberId, LocalDate, LocalDate)` implemented with a single `NamedParameterJdbcTemplate` SQL query:
`SELECT e.id FROM events WHERE event_date BETWEEN :from AND :to AND (coordinator_id = :memberId OR EXISTS (SELECT 1 FROM event_registrations WHERE event_id = e.id AND member_id = :memberId))`.
Date-range predicate reuses the same column semantics as `EventFilter.withDateRange` (maps to `event_date >= from AND event_date <= to`).

**Change 2 — Item filter pushed into `CalendarRepository`.**
New method `findEventDateItemsByDateRangeAndEventIds(from, to, Set<EventId>)` added to `CalendarRepository` + `CalendarJdbcRepository` (SQL: `WHERE kind = 'EVENT_DATE' AND event_id IN (:ids) AND date-overlap`). Short-circuit: when `eventIds` is empty, returns `List.of()` without hitting DB. `CalendarManagementService.filterForMySchedule` eliminated — the service now calls the new repo method directly. The in-Java `HashSet.addAll` union is gone.

**Change 3 — Controller early-return eliminated.**
Duplicate link-building branch removed from `CalendarController`. Port signature changed: `listCalendarItems(..., boolean myScheduleRequested, @Nullable MemberId myScheduleMemberId)` — two explicit parameters instead of overloading `null` with two different meanings. When `mySchedule=true && no member profile`, controller passes `(true, null)` → service short-circuits via empty `Set.of()` → repo returns empty without DB call. Single happy-path link-building now handles all cases.

**Tests:** All 64 calendar module tests + 56 events module tests + 9 integration tests green (129/129). Existing test `shouldReturnEmptyWhenMyScheduleTrueAndNoMemberProfile` updated: now verifies the service IS called with `(true, null)` and returns empty (no longer `verifyNoInteractions`).

---

### 2026-05-20 — Backend simplification finding H: CalendarFilter record

`CalendarManagementPort.listCalendarItems` previously spread five parameters across every layer (`startDate`, `endDate`, `sort`, `boolean myScheduleRequested`, `@Nullable MemberId myScheduleMemberId`). These domain inputs are now bundled into a `CalendarFilter` record in `com.klabis.calendar.application`. The port signature became `listCalendarItems(CalendarFilter filter, Pageable pageable)` — `Pageable` kept separate as a framework-driven concern; `Sort` moved inside `CalendarFilter` because it is domain input. `CalendarController` constructs one `CalendarFilter` instance and delegates; `CalendarManagementService` unpacks it. The null-means-no-profile semantic for `myScheduleMemberId` is unchanged. All 109 affected tests updated and green.

---
