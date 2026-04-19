## Why

GitHub issue #88 ("chci vedet kam jsem prihlasen", milestone `core`, labels `clen klubu`, `Events`, `hostujici clen`) asks for a way for a member to see every event they are currently registered to. The acceptance criterion says: *"v seznamu akcí si chci vyfiltrovat pouze akce na které jsem přihlášen"*.

Today the `event-registrations` spec only exposes the per-event view — `View Own Registration` returns the registration details for one specific event. There is no mechanism on the events list that lets a member collapse the list down to the events they are registered to. The `events` spec `List Events` requirement lists filters for status, organizer, date range, and coordinator, but nothing for registered-by-me. A member who wants to double-check "am I signed up for this weekend?" has to open each event's detail page in turn, or rely on the per-event `View Own Registration` page which requires knowing the event up front.

The calendar does not solve this — calendar items represent the *club's* events and their registration deadlines; they do not encode whether the viewing member actually has a registration.

## Capabilities

### New Capabilities

<!-- None. This change extends an existing capability. -->

### Modified Capabilities

- `events`: extend the `List Events` requirement with a new "only events I'm registered to" filter (toggle). Extend the `Events Table Display` requirement with the filter control on the events list filter bar.

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — `List Events` gains a new filter dimension and associated scenarios (filter on, filter off, combined with other filters, empty result, interaction with DRAFT visibility). `Events Table Display` gains a scenario for the new filter control in the filter bar.

**Affected code (backend, events module):** the events list query path must accept a new optional boolean filter and restrict the result set to events the authenticated user is registered to. The exact boundary between the `events` and `event-registrations` modules (a JOIN in the events JDBC query vs. the application service calling the registrations port for a set of event IDs) is an open design question (see Open Questions).

**Affected code (frontend):** the events list page filter bar gains a new toggle/checkbox. State plumbs into the existing events query as a query parameter.

**APIs (REST):** additive — new optional query parameter on `GET /api/events`. HAL-Forms affordance for the new filter is attached to the events list representation. No breaking change.

**Dependencies:** none added or removed.

**Data:** none — the filter reuses existing registration records.

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
