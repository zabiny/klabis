## Why

Issue #109 ("Chci mit moznost filtrovat v kalendari podle ruznych kriterii", milestone `MVP`) opens the umbrella discussion about calendar filtering. One concrete filter from its candidate list — "only items where the user is involved in the originating event" — has a clear, isolated scope and does not depend on any other open proposal. This change ships that single filter ahead of the broader umbrella.

Today `View Calendar Items` shows every calendar item in the selected date range to every authenticated member. A regular member who wants to see only events they actually have a stake in (registered as participant, or acting as coordinator) has to scan a noisy list of every club event, deadline reminder, and manual entry.

This filter introduces a "Můj rozvrh" toggle — "where I will physically be / where I have a responsibility" — narrowing the calendar to event-date items the user is personally involved with.

The filter is intentionally narrow: it does **not** cover family-member registrations, group relevance (that is `#110`), or deadline reminders (those are a TODO, not a schedule). Future expansions of "Můj rozvrh" (trainings, club meetings the user attends) can plug into the same toggle without renaming or rescoping.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `calendar-items`: extend `View Calendar Items` with an optional "Můj rozvrh" filter that restricts the returned calendar items to event-date items linked to events where the current user is either a participant with an active registration, or the event coordinator.

## Impact

**Affected specs:**
- `openspec/specs/calendar-items/spec.md` — `View Calendar Items` gains the "Můj rozvrh" filter with scenarios covering participant match, coordinator match, item-type exclusions (deadline and manual items hidden), and historical-view behaviour.

**Affected code (backend, calendar module):**
- Calendar list query accepts an optional `mySchedule` boolean `@RequestParam` on `GET /api/calendar`.
- When `mySchedule=true`, the calendar use case queries the events module twice via the existing `EventFilter` (once with `withRegisteredBy(currentUser)`, once with `withCoordinator(currentUser)`, both with `withDateRange`), unions the resulting event IDs, and restricts calendar items to event-date items linked to that set. Deadline items and manual items are excluded entirely when the filter is ON.

**Affected code (frontend):**
- Calendar page gains a "Můj rozvrh" toggle control, default OFF, state persisted in the URL.
- The toggle is always visible in the calendar header, so when the filter is ON and the grid renders empty, the user can connect "empty month" with "filter is active" and turn it off from the same place. No dedicated empty-state banner or message is introduced.

**APIs (REST):** additive — new optional query parameter `mySchedule=true` on `GET /api/calendar`. Default behaviour (parameter absent) is unchanged.

**Dependencies:**
- Uses the existing `EventFilter.withRegisteredBy` and `EventFilter.withCoordinator` ports on the events module. No new port introduced.

**Data:** none. No new tables, no denormalization. Live join via two queries per request.

## Resolved Decisions

- **`EventFilter.withRegisteredBy` semantics.** Confirmed during exploration: `EventRegistration` has no status field — registration cancellation is a row delete. The existing `withRegisteredBy(memberId)` query (`EXISTS (SELECT 1 FROM event_registrations …)`) therefore already means "has an active registration". No change to the events module is required. Cancelled events automatically drop out of "Můj rozvrh" because their calendar items are removed (per existing `calendar-items` spec). Finished events with a live registration remain — matches the agreed historical-view behaviour.

- **API shape: plain query parameter, no HAL-Forms affordance.** `mySchedule` is exposed as a plain optional `@RequestParam` on `GET /api/calendar`, mirroring how the events list exposes its filters (`registeredBy`, `coordinator`, `dateFrom`, …). HAL-Forms affordances stay reserved for mutating actions (POST/PUT/PATCH/DELETE). The frontend toggle constructs the URL explicitly; no server-side form metadata is needed.

- **No dedicated empty-state copy.** The calendar is rendered as a month grid, not a list — there is no natural "empty rows" surface. When the filter yields no items, the grid simply renders without item chips. Instead of an empty-state message, the active filter is always visually indicated in the header (the toggle control or a chip), so the user can connect "empty month" with "filter is on" and disable it from the same place. This replaces what would otherwise be a banner or text-state question.

- **Coordinator wording in spec stays unqualified.** Spec scenarios refer to "the event coordinator" without primary/deputy qualification. Today this maps to the single coordinator role implemented by `EventFilter.withCoordinator`. Proposal `#83` (deputy coordinator) is currently a `question` and not stabilised; when it lands, it carries the responsibility for deciding whether deputies are part of "Můj rozvrh" — either by extending `EventFilter.withCoordinator` to cover both roles, or by adding its own scenario to the `calendar-items` spec, or by explicitly excluding deputies. This change does not prejudge that decision.

- **Filter persistence is URL only.** The `mySchedule` toggle state lives only in the URL query string (consistent with how the events list and other filters work in the app today). Closing and reopening the calendar resets it to OFF. Cross-device or cross-session persistence (server-side preference, or local-storage like the recently-added table sort preference) is out of scope; a future proposal can introduce it if usage shows it is needed.

## Open Questions

<!-- All open questions have been resolved during exploration. See "Resolved Decisions" above. -->

