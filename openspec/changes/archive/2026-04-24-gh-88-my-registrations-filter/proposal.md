## Already delivered in `gh-88-events-list-filters`

The filter-bar slice of this proposal has already been implemented as change `gh-88-events-list-filters` (2026-04-24). Specifically:

- **"Moje přihlášky" toggle on the events list** — visible to users with a member profile, hidden otherwise. Backed by a `registeredBy=me` query parameter on `GET /api/events`; the backend resolves `me` to the acting user's `MemberId` and applies an EXISTS subquery against the `event_registrations` child table.
- **Default "Budoucí" time window and fulltext search (`q`)** — landed alongside the Moje toggle to give the events list a usable filter bar rather than three separate partial UIs.
- **`organizer` and `coordinator` query parameters** — wired through the controller (they existed in the spec but had no controller binding before this change).

Remaining scope that this `gh-88-my-registrations-filter` proposal should still cover:

- Dashboard widget "Moje nejbližší přihlášky" (next N events I'm registered to), if desired.
- Dedicated "Další závody" widget or a dashboard tile with related discovery.
- Proxy-registration semantics once issue #54 lands (`registeredBy=<memberId>` for members a user can act for).
- HAL-Forms search affordance for list filters — tracked as a separate foundation task (see `openspec/changes/gh-88-events-list-filters/tasks.md` 6.2).

Open Questions 1 (filter placement) and 7 (dashboard integration) from the original proposal are answered by the delivered slice: the events-list filter bar is built (Option A + partial Option C); the dashboard widget stays in this proposal's scope as the only remaining Option C piece.

## Why

GitHub issue #88 ("chci vedet kam jsem prihlasen", milestone `core`, labels `clen klubu`, `Events`, `hostujici clen`) asks for a way for a member to see every event they are currently registered to. The acceptance criterion says: *"v seznamu akcí si chci vyfiltrovat pouze akce na které jsem přihlášen"*.

Today the `event-registrations` spec only exposes the per-event view — `View Own Registration` returns the registration details for one specific event. There is no mechanism on the events list that lets a member collapse the list down to the events they are registered to. The `events` spec `List Events` requirement lists filters for status, organizer, date range, and coordinator, but nothing for registered-by-me. A member who wants to double-check "am I signed up for this weekend?" has to open each event's detail page in turn, or rely on the per-event `View Own Registration` page which requires knowing the event up front.

The calendar does not solve this — calendar items represent the *club's* events and their registration deadlines; they do not encode whether the viewing member actually has a registration.

## Capabilities

### New Capabilities

- `dashboard`: a new UI capability describing what the user sees on the home dashboard page after login. First widget is "Moje nadcházející akce" — the next three events the current user is registered to, with a link into the filtered events list for the full view. Widgets appear based on whether they apply to the current user (members see the registrations widget; users without a member profile do not).

### Modified Capabilities

<!-- None. The events-list filter bar slice was delivered in `gh-88-events-list-filters` and is already reflected in the `events` spec; the remaining scope is the dashboard widget, covered by the new `dashboard` capability above. -->

## Impact

**Affected specs:**
- `openspec/specs/dashboard/spec.md` (new) — describes the home dashboard page, the "Moje nadcházející akce" widget, visibility rules, empty state, and the shortcut into the events list.

**Affected code (backend):** new `DashboardController` in `common.ui` exposing `GET /api/dashboard` as a HAL link index. The `events` module contributes an `upcomingRegistrations` link via a postprocessor when the current user has a member profile; the link's target is a pre-built `/api/events` query. No events-module domain changes.

**Affected code (frontend):** `UserDashboard` component fetches `/api/dashboard`, conditionally renders the "Moje nadcházející akce" widget based on the `upcomingRegistrations` link presence, and follows that link to load widget data. `mockMyEvents` is removed from `mockDashboardData.ts`.

**APIs (REST):** additive — new `GET /api/dashboard` resource. No change to `/api/events` or any other existing endpoint.

**Dependencies:** none added or removed.

**Data:** none — the widget reuses existing registration records via the events-list query.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Filter placement — existing events list or dedicated "Moje přihlášky" page?**
   - Option A: extend the existing events list with a toggle, reusing all existing columns, affordances, and paging. This is the most direct interpretation of the acceptance criterion wording ("*v seznamu akcí si chci vyfiltrovat*").
   - Option B: introduce a separate `my-registrations` capability and a dedicated page. This enables registration-specific columns (the member's own category, their SI card number, registration timestamp) which do not fit the generic events list.
   - Option C: both — the filter on the events list for discoverability, plus a dashboard widget for quick access.

2. **Who counts as "I"?** The issue carries both `clen klubu` and `hostujici clen` labels. Should the filter reflect only registrations where the current user IS the registered member (registration.memberId == currentUser.memberId), or should it also reflect registrations the current user *made* on behalf of someone else (e.g., a parent registering a child, once that becomes possible per issue #54)? Today the spec only supports self-registration, so this can be deferred — but we should be explicit.

3. **Interaction with status filter and past events.** Should the filter be a pure "subset" of whatever status filter the user has applied (so "status=FINISHED AND onlyRegistered=true" gives past events I attended), or should it force a specific status behavior (e.g., "hide FINISHED by default")? The simplest answer is "purely additive — respects all other active filters". Confirm this is intended.

4. **DRAFT event visibility.** Regular members cannot see DRAFT events today and cannot register to them. Should the new filter be visible at all to members without `EVENTS:MANAGE`, and if so, does it just silently exclude DRAFT events (same as today)? Managers with `EVENTS:MANAGE` can see DRAFTs but also cannot be registered to one. Recommend: filter available to everyone, DRAFT exclusion unchanged.

5. **Backend query boundary.** Two viable implementations:
   - (a) The events list JDBC query joins against the registrations table directly. Paging/sorting are straightforward; modules become coupled at the persistence layer.
   - (b) The events application service asks the registrations port for the current user's registered event IDs and passes them to the existing list query as an extra `eventIds IN (...)` filter. Preserves module isolation; paging/sorting still work because the events module remains the primary query.

   Which is preferred given backend-patterns conventions in this project?

6. **Does this filter need its own HAL-Forms affordance, or is a plain query parameter enough?** The existing list filters (status, organizer, date range, coordinator) — how are those exposed today? Should this filter mirror the same pattern exactly, or is it an opportunity to add a proper HAL-Forms affordance for all list filters at once (scope creep risk — probably not).

7. **Dashboard integration — in scope for this change or separate?** A natural follow-up is a "Moje nejbližší přihlášky" widget on the dashboard showing the next N events where the member is registered. Issue #88 does not mention the dashboard. Recommend: *out of scope* for this change. Confirm.
