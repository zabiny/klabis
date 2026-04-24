## Context

The members list today supports pagination and sort via `PageableDefault(sort = "lastName", direction = ASC)` in `MemberController.listMembers()`. There is no filter bar; the only filter dimension is a boolean `onlyActive` on `MemberFilter`, forced `true` for non-MANAGE callers by the application service. Users look up members by pagination alone, which is increasingly painful at several hundred rows.

This change mirrors the filter-bar pattern shipped in `gh-88-events-list-filters`. The technical substrate (`unaccent` PostgreSQL extension with an H2 alias, Flyway migration V004) is already in place. What's left is applying the same shape to members — and extracting the shared pieces rather than copy-pasting.

## Goals / Non-Goals

**Goals:**

- A filter bar on the members list with two controls (fulltext, status pill group).
- A useful default view — "Aktivní" — so a manager opening the page sees the working set, one click away from deactivated members. Matches the "useful default" choice made for events (`Budoucí`).
- A REST API that honors the new filter dimensions and enforces the authority gate server-side.
- Extract a shared `<FulltextSearchInput />` component that events + members consume, so debounce and URL binding live in one place.
- Capture the Klabis frontend UI patterns used by the filter bars in a short documentation file so future developers can align without reading two proposals.

**Non-Goals:**

- Introducing a `MemberStatus` enum on the `Member` aggregate. The filter uses its own 3-valued enum; the aggregate's `boolean active` stays unchanged.
- HAL-Forms search template affordances for list endpoints — tracked as a separate foundation task (`tasks/hal-forms-search-affordance-foundation.md`).
- "My"-scoped filters (family group, training group). Those are cross-module queries that belong in a separate proposal.
- Contextual empty-state copy.

## Decisions

### Fulltext: substring match on firstName + lastName + registrationNumber, same SQL shape as events

Case-insensitive and diacritics-insensitive (`unaccent(LOWER(col)) LIKE '%' || unaccent(LOWER(:token)) || '%'`). Multi-word queries AND across tokens (split on whitespace); each token OR'd across all three columns. One SQL expression handles names and registration numbers because `unaccent(lower("ZBM9500"))` is just `"zbm9500"` — no special case needed for the ASCII-only reg-number column.

**Rationale:**

- Czech surnames (Čermák, Šebek, Žáček) are routinely typed without háčky on mobile; plain `ILIKE` would miss.
- The user asked for a single search field over all three columns, not a dedicated reg-number input.
- Consistent with events' fulltext — same SQL primitive, same min-length rule, same debounce, same `FulltextSearchInput` React component.

**Alternatives considered:**

- **Prefix match for registration numbers** (detect `ZBM*` pattern → use `LIKE 'zbm%'`, else substring). Rejected as premature cleverness: users who type "95" reasonably expect to find `ZBM9500`-era members; adding a mode-switching heuristic adds complexity without clear user value.
- **Separate reg-number input.** Rejected: clutters the UI and defeats the "one search box" mental model.
- **Postgres tsvector with stemming.** Overkill for <10k rows; stemming can surprise users looking for literal fragments.

**Implementation detail:** min 2 characters after trim (same as events — "5" would be far too noisy; "95" or "Čer" is the minimum useful query).

### Status pill group for MEMBERS:MANAGE only, default "Aktivní" for all users

The pill group exposes three values: **Aktivní / Neaktivní / Vše**. Visible **only** when the current user has MEMBERS:MANAGE authority — hidden for regular members. The default selection is "Aktivní" for everyone, which matches the current spec for regular users and deliberately narrows the default view for managers (visible behavior change; acceptable pre-production).

**Rationale for "Aktivní" default for managers:**

- Most of the time a manager is working with active members (registering them, editing their profiles, assigning permissions). Deactivated members are the exception.
- One click away — the manager who needs to find a deactivated member flips the pill.
- Consistent with the events "Budoucí" default philosophy (useful defaults over "show everything").

**Why pill group over checkbox:**

- Symmetry with the events time-window widget.
- Three discrete values (ACTIVE / INACTIVE / ALL) map naturally to three pills; collapsing to a checkbox would force the awkward "only deactivated" state into a negative toggle.
- The upcoming `docs/frontend-patterns.html` documents "3-valued exclusive selector" as the pill-group pattern, reused across events and members.

**Alternatives considered:**

- **Default "Vše" for managers** (today's behavior). Rejected — at odds with the "useful default" principle; makes the filter bar decorative for managers on first load.
- **Checkbox "Show deactivated".** Rejected — asymmetric (no way to see only deactivated) and loses the direct alignment with the events widget.
- **Dropdown.** Rejected — less discoverable than a pill group for a binary-ish decision.

### Remove the per-row status column from the table

With the status pill group in the filter bar, the per-row active/inactive column becomes redundant. It's removed for everyone:

- Regular users never saw the column (current spec already hides it for non-MANAGE users).
- Managers now always know which bucket they're looking at — it's the active pill.
- Mixing active and inactive members in the same view only happens when the manager explicitly selects "Vše"; the visual distinction in that narrow case (if needed) can be expressed via row styling or a row-level action set rather than a dedicated column.

Action affordances that currently vary by status (suspend / resume, permissions link visibility) stay in the action column as they are — their presence encodes the member's status for the rows where it matters.

**Alternatives considered:**

- **Keep the status column for managers, hide it for regular users** (today's behavior). Rejected — duplicates information already shown by the filter bar selection, and pushes the table wider for no real benefit.
- **Status badge on the name instead of a dedicated column.** Rejected as unnecessary visual noise in the 99% case where a manager is looking at a single-status view.

### Filter-only status enum on the REST API and `MemberFilter`

The REST API introduces an enum with three values: `ACTIVE`, `INACTIVE`, `ALL`. `MemberFilter` gains a new field of the same shape. The `Member` aggregate's `boolean active` stays untouched; no domain-level `MemberStatus` enum.

**Rationale:**

- Minimal domain churn. Introducing a real enum on the aggregate would touch `Member`, `MemberMemento`, persistence mapping, and every existing caller — out of proportion to what this change is trying to deliver.
- Forward-compatible. If a full `MemberStatus` enum lands later (e.g. adding `PENDING` or `EXPELLED`), the filter-only enum can be renamed or promoted; the API shape (`?status=ACTIVE`) stays.
- Matches the events precedent of introducing the API shape for `registeredBy=me` without unnecessarily remodeling the domain.

**Alternatives considered:**

- **Full enum on the aggregate.** Rejected as premature — no product driver.
- **Nullable boolean on the filter** (`Boolean activeFilter`, null = all). Rejected — less symmetric with the 3-pill UX and less self-documenting in the API than a named enum value.

### Non-MANAGE users: silently ignore the `status` param

A regular user sending `?status=INACTIVE` (URL typed by hand, tampered client) gets active-only results — the param is silently forced to `ACTIVE`. No 400 or 403 is raised.

**Rationale:**

- The UI hides the pill group for non-MANAGE users, so the API branch is only reachable by direct calls or URL tampering.
- The existing spec rule "regular users see only active members" stays authoritative; no new error path to test.
- Matches the events precedent (`registeredBy=me` for users without a member profile → silent empty result, not 400).
- If a regular user later gains MEMBERS:MANAGE the same URL just starts returning the richer result set — no surprise error on transition.

**Alternative considered:** 400 or 403. Rejected — treats a low-value API corner as a contract violation, adds an error path to test, and doesn't serve any user flow.

### URL query parameters are the single source of truth

Filter state lives in the URL. Defaults are implicit (no query param = default value, except "Aktivní" is injected on first render via `useSearchParams` with `replace:true` so the default is explicit in the URL after the first render). Page reload, back/forward, and link sharing all just work. No session storage, no per-user server-side preference.

Same pattern as events.

### Default sort adds firstName ASC as secondary

The current primary sort `lastName ASC` stays. `firstName ASC` is added as a secondary tie-break, so two members with the same surname appear in a deterministic order. User-clicked column sort overrides both.

This is a small backend-only change in `MemberController` (`PageableDefault(sort = ...)`) and the JDBC adapter's allow-list (`ALLOWED_SORT_FIELDS` already contains `firstName`).

### Shared `<FulltextSearchInput />` component

A single React component used by both filter bars. Encapsulates:

- Local input state (immediate responsiveness).
- 250 ms debounce.
- Min-2-chars rule after trim.
- URL query-param binding via `useSearchParams`.
- A `paramName` prop (events uses `q`, members uses `q` — same in practice, but the prop keeps the component reusable).

The existing `EventsFilterBar` is refactored to consume it (inline debounced input removed). The members filter bar consumes the same component.

**Why extract now (with only two consumers):**

- The two implementations are close to identical in behavior; copy-paste is an obvious smell.
- Extraction happens alongside the members implementation — a single reviewable diff, no follow-up cleanup task.
- The HAL-Forms affordance foundation is queued; when it lands it will need a single shared component to drive the search input from the template, not two.

### Filter-only enum decoded in the controller, not the domain

The REST `status` parameter is decoded into the filter-only enum in `MemberController` (or a small private helper). The domain-level `MemberFilter` carries the enum as-is. The repository adapter translates it to the SQL predicate (`active = true` for ACTIVE, `active = false` for INACTIVE, no predicate for ALL).

Keeps request-parsing concerns in the web layer; domain stays infrastructure-agnostic.

## Risks / Trade-offs

- **Default view change is visible to every MEMBERS:MANAGE user.** → Acceptable: pre-production, no integrations rely on "all by default". The narrower default matches what managers actually do most of the time.
- **Sequential scan on `unaccent(LOWER(...))` across three columns.** → Acceptable at expected member counts (hundreds, low thousands). A functional GIN index is an upgrade path if latency becomes an issue — same trade-off as events.
- **Shared component extraction may slow the members implementation.** → Mitigated by keeping the extraction minimal (one component, one prop) and doing it in the same change so scope stays visible.
- **"Filter-only" enum drifts from a future aggregate enum.** → The change captures the decision explicitly; if a real `MemberStatus` enum lands later, it's a clean rename/merge, not a redesign.
- **`docs/frontend-patterns.html` becomes stale.** → Mitigated by placing it next to other living docs (`docs/EVENT-DRIVEN-ARCHITECTURE.md`) and keeping it short (patterns only, no pros/cons essays).

## Migration Plan

Forward-only deployment:

1. Backend: extend `MemberFilter`, `MemberRepositoryAdapter`, `MemberController`. No Flyway migration — `unaccent` is already enabled.
2. Frontend: extract `<FulltextSearchInput />`, refactor `EventsFilterBar` to consume it, build the members filter bar.
3. Docs: add `docs/frontend-patterns.html` capturing the pattern catalogue.

Rollback: revert the deployment. No data writes.

## Open Questions

None — all decisions confirmed during the explore session.
