## Context

The events list is the primary way members discover club events. Today it supports basic status filtering via URL query param, but the larger requirement set in the existing `List Events` spec (organizer, date range, coordinator) is not wired through the controller. The events table also has no default time horizon, no text search, and no "only events I'm registered to" filter — the three user-facing gaps this change closes.

`EventFilter` (a value object in the events domain) already models the query shape: status set, organizer, dateFrom/dateTo. The `EventRepositoryAdapter` (JDBC) turns it into SQL. The `Event` aggregate root contains `EventRegistration` children directly — registrations are not a separate module from a persistence viewpoint, which simplifies the "Moje přihlášky" query considerably: it is a filter on the current aggregate's child collection, not a cross-module concern.

The calendar bounded context (capability `calendar-items`) has parallel but separate user-filter work underway (issues #109, #110). This design does not touch the calendar.

## Goals / Non-Goals

**Goals:**

- A filter bar on the events list with three new controls (fulltext, time window, my-registrations toggle).
- A useful default view — "Budoucí" (upcoming) — so a freshly opened events list shows what's relevant to members.
- A REST API that honors every filter dimension the spec describes, including the four that exist in spec today but are not wired through the controller (organizer, coordinator, dateFrom, dateTo).
- Forward compatibility on proxy registrations (#54) via the `registeredBy=me` shape.
- Minimal coupling: extend existing `EventFilter` + `EventRepositoryAdapter`, don't introduce new services.

**Non-Goals:**

- Introducing HAL-Forms search affordances. The current pattern in Klabis is plain query parameters for list filtering, HAL-Forms templates are reserved for write affordances. Migrating list pages to HAL-Forms search templates is valuable but is a foundation change that warrants its own proposal — tracked separately in the task queue.
- Dashboard integration ("Moje nejbližší přihlášky" widget). Stays in the broader `gh-88-my-registrations-filter` proposal.
- Calendar filters (issues #109, #110).
- Proxy registration (registering on behalf of someone else). Issue #54 tracks that; here we only lay the API shape.
- Full-text search with stemming (`tsvector`), trigram similarity, or language analysis. Substring with `unaccent` is the target depth.

## Decisions

### Widget "Zobrazit" maps to existing `dateFrom`/`dateTo`, no new backend dimension

The three-value time-window selector (Budoucí / Proběhlé / Vše) is a **frontend convenience** over the existing `dateFrom`/`dateTo` query params. The backend sees:

| Widget value | `dateFrom` | `dateTo` |
|--------------|------------|----------|
| Budoucí      | today      | —        |
| Proběhlé     | —          | today - 1 |
| Vše          | —          | —        |

Today's events count as "Budoucí". This matches the existing automatic-completion logic (events transition to FINISHED only after the event date has passed, i.e. the next day).

**Alternatives considered:**

- A domain-level `timeWindow` enum in `EventFilter`. Rejected: adds a redundant dimension (same information is already expressible via date bounds), forces all internal callers to learn a new concept, and complicates SQL generation (two fields instead of one). The widget is a UI affordance, not a domain concept.
- Hard-coding "FUTURE" as a server-side default when no date params are given. Rejected: masks the contract (a client calling with no params gets different data than a client calling with explicit `dateFrom=<some date>`), which is confusing for automation/testing and surprising for spec readers.

**Consequence:** The domain stays lean, the spec requirement `Filter events by date range` already covers the backend behavior, and the widget is an implementation detail of the frontend.

### "Moje přihlášky" is a filter on the Event aggregate's child registrations, not a cross-module concern

`Event` aggregate owns `EventRegistration` children. The events list repository already loads the aggregate. Adding "only where a child registration's memberId equals the current user's memberId" is a single-aggregate query. SQL approach:

```sql
-- pseudo; actual expression in EventRepositoryAdapter
WHERE (:registeredBy IS NULL OR EXISTS (
  SELECT 1 FROM event_registrations er
  WHERE er.event_id = e.id AND er.member_id = :registeredBy
))
```

The query shape naturally supports the spec's "status of the event is irrelevant" rule — CANCELLED and FINISHED events with a live registration are returned just like ACTIVE ones.

**Alternatives considered:**

- Pre-fetch registered event IDs through a port and pass an `IN (...)` list. Would be the right call if registrations were a separate aggregate in a different module. They are not.
- Read-model denormalization (events cache which members are registered). Rejected as premature optimization; a single EXISTS clause on an indexed `(event_id, member_id)` pair is cheap on any realistic dataset.

### `EventFilter` gains two fields: `fulltextQuery` and `registeredBy`

```
EventFilter(
  statuses: Set<EventStatus>,
  organizer: String?,
  dateFrom: LocalDate?,
  dateTo: LocalDate?,
  fulltextQuery: String?,    // NEW
  registeredBy: MemberId?,   // NEW
  coordinator: MemberId?     // NEW if missing today — verify during implementation
)
```

`fulltextQuery` holds the raw user query after trim; the repository adapter is responsible for tokenization and SQL expansion (AND across tokens, each token OR'd across `name` and `location`, both sides `unaccent(LOWER(...))`).

`registeredBy` holds a `MemberId`. The controller resolves the API parameter `registeredBy=me` to the current user's member id; other values are currently rejected (forward-compatible on #54).

The existing factory methods (`byStatus`, `byOrganizer`, `byDateRange`) stay as-is. Fluent "withers" may be needed to combine criteria — detail for implementation.

### Fulltext: substring match on name + location, with diacritics normalization

Case-insensitive and diacritics-insensitive (`unaccent(LOWER(name)) LIKE '%' || unaccent(LOWER(:token)) || '%'`). Multi-word queries AND across tokens (split on whitespace).

**Rationale:** Czech place names (*Šárka, Čeřínek, Ždár*) are commonly typed without diacritics on mobile; a plain `ILIKE` would surprise users. PostgreSQL `unaccent` extension ships with the standard image; if not already enabled in Klabis migrations, we add it.

**Alternatives considered:**

- Postgres `tsvector` full-text search with a Czech configuration. Overkill for <5k rows, heavier setup (GIN indexes, migration, text config), and stemming behavior surprises users more often than it helps (they search for exact place names, not concepts).
- `pg_trgm` similarity. Ranked fuzzy results are nice but noisy (*"proč se mi zobrazuje akce kterou jsem nehledal?"*) and overkill at this scale.

**Implementation details (not in spec):**

- Min length: 2 characters. Shorter input is ignored by the frontend (not sent to the API). One character matches tens of thousands of rows and saturates the UI with no user value.
- Debounce: ~250ms on frontend input.
- Trim leading/trailing whitespace.

### Default sort follows the time window

| Widget value | Default sort     |
|--------------|------------------|
| Budoucí      | eventDate ASC    |
| Proběhlé     | eventDate DESC   |
| Vše          | eventDate DESC   |

The frontend controls this by setting the `sort` query parameter appropriately whenever the time window widget changes and no explicit user sort is set (via column-header click). The backend default (`eventDate DESC`) stays unchanged — it's the fallback when the frontend sends no `sort`, and it matches the API's current behavior.

### URL query parameters are the single source of truth

Filter state lives in the URL. Defaults are implicit (no query param = default value). Page reload, back/forward, and link sharing all just work. No session storage, no per-user server-side preference.

### User without a member profile gets a silent no-op, not 400

If `registeredBy=me` is sent by a user who has no member profile, the backend returns 200 with an empty result rather than 400. The UI hides the "Moje" toggle for such users, so this path is only reachable by direct API calls — where a polite no-op is less surprising than an error. It also means automation can safely pass `registeredBy=me` without branching on "does this user have a profile".

**Alternative considered:** 400 Bad Request with a machine-readable error. Rejected because it treats a low-value API corner as a contract violation, adds an error path to test, and doesn't serve any user flow (the UI already suppresses the toggle).

### New REST API query params

Additive on `GET /api/events`:

- `q` — fulltext query string.
- `registeredBy` — only `me` is currently accepted. Other values are rejected as 400. Future (#54): accept any memberId the caller is authorized to act for.
- `organizer` — existing spec dimension not previously wired.
- `coordinator` — existing spec dimension not previously wired.

`dateFrom`, `dateTo`, `status` are unchanged.

All params are optional. Any combination is accepted; they AND together.

## Risks / Trade-offs

- **Default view change is visible to every user.** → Acceptable: Klabis is pre-production, no integrations rely on today's "show everything" default. The new default is what users expect from an events list (*"what's coming up"*).
- **`unaccent` extension may not be enabled in existing migrations.** → Mitigation: implementation adds a Flyway migration (or extends the existing V001) to ensure `CREATE EXTENSION IF NOT EXISTS unaccent`. H2 (used in dev/test) doesn't have `unaccent`; a small abstraction or a custom H2 function alias will be needed so tests pass.
- **Multiple AND'd LIKE clauses on `unaccent(LOWER(...))` prevent index use.** → Acceptable: on the expected row count (hundreds, thousands worst case), a sequential scan is still well under the 500ms budget. A functional GIN index on `unaccent(lower(name) || ' ' || coalesce(lower(location),''))` is the upgrade path if latency becomes an issue.
- **"Moje" + "Budoucí" may commonly return zero for new members.** → Mitigation: generic empty-state message. The CTA-style empty state (*"Show all upcoming club events"*) is a cheap follow-up we can add without spec change.
- **User bumps into the default "Budoucí" view, tries to find a past event, doesn't know the time-window widget exists.** → Mitigation: filter bar is always visible, widget is a prominent pill group, default "Budoucí" is labeled clearly.

## Migration Plan

No data migration needed. The change is a forward-only deployment:

1. Backend: extend `EventFilter`, `EventRepositoryAdapter` (JDBC query), `EventController`. Add Flyway migration for `unaccent` (PostgreSQL); H2 alias for tests. Ship together.
2. Frontend: add filter bar component, wire URL query params, translate widget to `dateFrom`/`dateTo`. Deploy.
3. Finalization: update `gh-88-my-registrations-filter/proposal.md` with "what's already delivered" note. Queue HAL-Forms search foundation as a separate task-queue item.

Rollback: revert the deployment. No data writes involved; safe.

## Open Questions

None — all design decisions confirmed during the explore session.
