## Why

The members list has no filter controls today. Regular users see only active members (all of them), managers see all members (active and inactive) — and neither group can narrow the list down. For a club with several hundred members the directory view is awkward: looking up a specific person means visually paginating through the whole table, and a manager who needs to audit only deactivated memberships has no way to isolate them.

Two gaps are cheap to close in the same filter-bar slice:

- **No fulltext search.** A user searching for *"Cermak"* or *"ZBM95"* has to scroll. Czech surnames need diacritics-insensitive matching (*"Cermak"* should match *"Čermák"*), and the registration number is a common lookup key for managers reconciling with ORIS or paper records.
- **No status filter for managers.** The spec today already says managers see *all* members; there's no way for them to isolate just the deactivated ones, or just active ones when they're working on an import/registration.

This proposal mirrors the filter-bar pattern shipped in `gh-88-events-list-filters`: URL query params as single source of truth, debounced fulltext input with a 2-character floor, a 3-valued pill group for a discrete filter dimension. Doing it now lets us extract a shared frontend component (`FulltextSearchInput`) rather than letting two filter bars drift.

## What Changes

- **Members list filter bar** above the table with:
  - **Fulltext search** — substring match on `firstName`, `lastName`, `registrationNumber`, case-insensitive and diacritics-insensitive. Multi-word queries AND across tokens; each token OR'd across the three columns.
  - **Status selector** — pill group "Aktivní / Neaktivní / Vše", visible **only** to users with MEMBERS:MANAGE authority. Default is "Aktivní" for everyone, matching today's behavior for regular users and narrowing the default view for managers.
- **Default view** for MEMBERS:MANAGE narrows from "all members" to "Aktivní" — a visible behavior change. Rationale is the same as the events `Budoucí` default: most of the time a manager is working with active members; finding deactivated ones is the exception and is one click away.
- **Status column removed from the members table.** With the status pill group in the filter bar, showing a per-row status column becomes redundant noise for managers (they know which status bucket they're looking at — it's the active pill), and regular users never saw the column. The column is dropped for everyone.
- **Default sort** gains `firstName ASC` as a secondary key (primary `lastName ASC` is unchanged) so ties in last name produce a stable order.
- **REST API `GET /api/members`** gains two additive optional query parameters:
  - `q` — fulltext query.
  - `status` — `ACTIVE` / `INACTIVE` / `ALL`. Silently ignored for callers without MEMBERS:MANAGE (forced to `ACTIVE`).
- **`MemberFilter` domain value object** gains `fulltextQuery` and a filter-only `StatusFilter` enum (ACTIVE / INACTIVE / ALL). The aggregate's `active` boolean is unchanged — no `MemberStatus` enum on the Member aggregate itself.
- **Shared `FulltextSearchInput` React component** — extracted from the events filter bar and reused on the members filter bar. Encapsulates the 250 ms debounce, min-2-chars rule, and URL query-param binding.
- **New documentation** `docs/frontend-patterns.html` capturing the Klabis frontend UI patterns that have emerged from the events + members filter bars (pill group, fulltext search, binary toggle, filter bar layout, default-filter injection, empty-state copy).

No breaking changes. The manager default-view shift is a visible behavior change; the app is pre-production and no integrations depend on the current default.

## Capabilities

### New Capabilities

<!-- None. This change extends an existing capability. -->

### Modified Capabilities

- `members`: `Member List` requirement gains filter dimensions (fulltext, status pill group for managers, default "Aktivní" for everyone, stable secondary sort).

## Impact

**Affected specs:**

- `openspec/specs/members/spec.md` — `Member List` requirement gains scenarios for fulltext (name + registration number, case-insensitive, diacritics-insensitive, multi-word AND), status pill group (manager-only, default "Aktivní"), regular user silent-ignore of the status param, secondary-sort fallback, empty-state message.

**Affected code (backend, members module):**

- `MemberFilter` — add `fulltextQuery: String?` and a filter-only `StatusFilter` enum field (ACTIVE / INACTIVE / ALL). Keep the existing `onlyActive` semantic or fold it into the new enum for callers — implementation detail.
- `MemberRepositoryAdapter` JDBC query — extend with `unaccent(LOWER(...))`-based LIKE across `first_name`, `last_name`, `registration_number`, AND-ing tokens split on whitespace, each token OR'd across the three columns. Status filter maps to a predicate on the existing `active` column.
- `MemberController.listMembers()` — two new `@RequestParam`s (`q`, `status`); authority-gated mapping (regular users' `status` value ignored).
- `unaccent` extension is already enabled (Flyway V004 from `gh-88-events-list-filters`). No new migration required.

**Affected code (frontend):**

- Members list page gains a filter bar with the fulltext input and (manager-only) pill group.
- New shared component `<FulltextSearchInput />` — debounced, min-2-chars, URL-bound.
- `EventsFilterBar` refactored to consume the shared component (remove the inline debounced input).

**APIs (REST):** additive — two new optional query parameters on `GET /api/members`. HAL-Forms search affordance is **not** part of this change (queued as separate foundation task).

**Documentation:**

- New file `docs/frontend-patterns.html` — catalogue of Klabis frontend UI patterns (filter bar components, defaults, empty-state copy). Expected to grow as further patterns emerge.

**Dependencies:** none added or removed. `unaccent` already enabled.

**Data:** no schema changes.

**Out of scope:**

- Introducing a `MemberStatus` enum on the `Member` aggregate (filter-only enum suffices).
- "My" toggles (family group, training group) — those are cross-module queries and belong in a separate change.
- HAL-Forms search template affordance — queued as a separate foundation task.
- Contextual empty-state copy per filter type — generic copy suffices; contextual is a cheap later follow-up.
