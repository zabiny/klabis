## Why

GitHub issue #88 ("chci vedet kam jsem prihlasen", milestone `core`, labels `clen klubu`, `Events`, `hostujici clen`) asks club members to be able to narrow the events list down to "only events I am registered to". The acceptance criterion is: *"v seznamu akcí si chci vyfiltrovat pouze akce na které jsem přihlášen"*.

While investigating that requirement we identified two further gaps that are cheap to close in the same filter-bar work and would otherwise create immediate follow-up churn:

- The events list has **no text search**, so a member looking for *"jihlava"* or *"Šárka"* has to paginate visually through every row. For a club accumulating several hundred events per season this is increasingly painful.
- The events list has **no notion of a default time horizon**. Every visitor sees every event in the database, including events from three years ago, which pushes upcoming events below the fold and makes the list effectively useless as a "what's coming up" view.

Addressing these three points together lets us land a coherent filter bar in one iteration rather than shipping three partial UIs. The broader umbrella proposal `gh-88-my-registrations-filter` (widget "Další závody", dashboard integration) stays open — this change tackles only the filter-bar slice.

## What Changes

- Events list page gains a **filter bar** visible above the table with three new controls:
  - **Fulltext search** — substring match on event name + event location, case-insensitive and diacritics-insensitive (so *"cernav"* finds *"Černava"*, *"sarka"* finds *"Šárka"*).
  - **Time window selector** — three-value control "Zobrazit: Budoucí / Proběhlé / Vše" with Budoucí as default. Today's events count as "Budoucí".
  - **My registrations toggle** — hides all events where the current user is not registered. Visible only to users who have a member profile.
- **Default view** of the events list is "upcoming only" (time window = Budoucí). A user opening `/events` without any explicit filters sees only events with `event_date >= today`.
- **Default sort** adapts to the time window — Budoucí sorts by `eventDate` ascending (nearest first), Proběhlé and Vše sort descending (most recent first). Users can still re-sort by clicking column headers.
- REST API `GET /api/events` gains additive optional query parameters: `q` (fulltext), `registeredBy` (with `me` as the only value for now, forward-compatible on #54), `organizer`, `coordinator`. The existing `dateFrom`/`dateTo` params are unchanged — the frontend maps the time window widget onto them.
- `EventFilter` domain value object gains `fulltextQuery` and `registeredBy` fields. The repository query for listing events extends to honor both.

No breaking changes. The default view shift (from "everything" to "Budoucí") is a visible behavior change, but the application is pre-production and no external integrations depend on the default.

## Capabilities

### New Capabilities

<!-- None. This change extends an existing capability. -->

### Modified Capabilities

- `events`: `List Events` gains filter dimensions (fulltext, time window default, registered-by-me, explicit organizer/coordinator query params). `Events Table Display` gains the filter bar and default-sort-follows-time-window scenarios.

## Impact

**Affected specs:**

- `openspec/specs/events/spec.md` — `List Events` requirement gains scenarios for fulltext search, default Budoucí view, registered-by-me filter, organizer/coordinator filters (these last two exist in the requirement but have no scenarios that match current API behavior). `Events Table Display` requirement gains scenarios for the filter bar presence and time-window-dependent default sort.

**Affected code (backend, events module):**

- `EventFilter` value object — two new fields (`fulltextQuery: String`, `registeredBy: MemberId`).
- `EventRepositoryAdapter` JDBC query — extended WHERE clauses for fulltext (unaccent-based), and EXISTS-subquery (or JOIN on the child registration table within the same aggregate) for registered-by filter.
- `EventController.listEvents()` — four new `@RequestParam`s (`q`, `registeredBy`, `organizer`, `coordinator`); map to `EventFilter` factory.
- DB migration — add `unaccent` PostgreSQL extension if not already present.

**Affected code (frontend):**

- Events list page gains a filter bar component with the three controls.
- Time window widget translates to `dateFrom`/`dateTo` query params (Budoucí → `dateFrom=today`, Proběhlé → `dateTo=today-1`, Vše → neither).
- URL query parameters are the single source of truth for filter state.
- `Moje` toggle hidden when the current user has no member profile.

**APIs (REST):** additive — four new optional query parameters on `GET /api/events`. HAL-Forms search affordance is **not** part of this change; a separate foundation task is queued to introduce that pattern across list endpoints.

**Dependencies:** none added or removed at the Gradle/npm level. PostgreSQL `unaccent` extension already ships with the standard image.

**Data:** no schema changes beyond enabling `unaccent`.

**Out of scope:**

- Dashboard widget "Moje nejbližší přihlášky" (stays in the broader `gh-88-my-registrations-filter` proposal).
- Calendar filters — tracked separately in `gh-109` and `gh-110`.
- Proxy registration (`registeredBy=<someMemberId>`) — tracked in issue #54; the `registeredBy=me` value is introduced now as a forward-compatible API shape.
- HAL-Forms search template affordance — tracked as a separate task-queue item for later foundation work.
