## 1. Backend foundation — domain & persistence for fulltext

- [x] 1.1 Write failing repository tests for fulltext filter on `firstName`, `lastName`, and `registrationNumber` (case-insensitive, diacritics-insensitive).
- [x] 1.2 Extend `MemberFilter` value object with `fulltextQuery: String?` field and a `withFulltext(String)` wither. Trim input; null when blank.
- [x] 1.3 Extend `MemberRepositoryAdapter` JDBC query with unaccent-based `LIKE` across `first_name`, `last_name`, `registration_number`, AND-ing tokens split on whitespace, each token OR'd across the three columns. Reuse the pattern from `EventRepositoryAdapter.findIdsByFulltext`.
- [x] 1.4 Green: tests for single-column matches pass.
- [x] 1.5 Add tests for multi-word AND semantics; make them pass.
- [x] 1.6 Refactor SQL composition to keep the adapter readable.

## 2. Backend foundation — status filter

- [x] 2.1 Write failing repository tests covering `StatusFilter` values ACTIVE, INACTIVE, ALL.
- [x] 2.2 Add a filter-only enum `MemberFilter.StatusFilter` (or equivalent location — implementation detail) with values ACTIVE, INACTIVE, ALL. `MemberFilter` gets a new field of this type (default ACTIVE for authenticated code paths; ALL for no-filter callers).
- [x] 2.3 Extend `MemberRepositoryAdapter` to translate the enum into the SQL predicate (`active = true`, `active = false`, no predicate for ALL).
- [x] 2.4 Update the existing `onlyActive`-based call sites: either keep `onlyActive` as a convenience constructor that maps to the new enum, or replace call sites — decide when implementing. `MembersImpl` / application services must still force non-MANAGE callers to the ACTIVE branch.
- [x] 2.5 Green tests.

## 3. Backend — REST controller wiring

- [x] 3.1 Write failing `@WebMvcTest` for `MemberController.listMembers` accepting `q` parameter and mapping it to the service as `MemberFilter.fulltextQuery`.
- [x] 3.2 Add `@RequestParam(required = false) String q` to `MemberController.listMembers`; map into `MemberFilter`.
- [x] 3.3 Write failing test for `status=ACTIVE/INACTIVE/ALL` when caller has MEMBERS:MANAGE — value passes through to the filter.
- [x] 3.4 Add `@RequestParam(required = false) String status`; parse into the filter-only enum. Reject unrecognized values with 400.
- [x] 3.5 Write failing test for authority gate: non-MANAGE caller sending `status=INACTIVE` or `status=ALL` still gets only active members (silent ignore).
- [x] 3.6 Implement the authority gate — when caller lacks MEMBERS:MANAGE, force the filter's status to ACTIVE regardless of the `status` param.
- [x] 3.7 Update the controller's `PageableDefault` (or the service/adapter default) to include `firstName ASC` as a secondary sort after `lastName ASC`. Write a test for the fallback ordering.
- [x] 3.8 Add an E2E test (`MemberFilterE2ETest`, patterned on `EventFilterE2ETest`) that exercises `q` and `status` through the full MVC stack against the dev-profile H2 DB — slice tests alone did not catch the analogous runtime bugs on events.
- [x] 3.9 Refactor controller to keep the listing method readable.

## 4. Frontend — shared FulltextSearchInput component

- [x] 4.1 Extract a `<FulltextSearchInput />` component from `EventsFilterBar.tsx` with props for URL param name, placeholder text, and the debounce/min-chars rules as defaults.
- [x] 4.2 Migrate `EventsFilterBar` to consume the shared component; existing events tests must still pass.
- [x] 4.3 Component tests for `<FulltextSearchInput />` (debounce, min-chars gate, URL binding).

## 5. Frontend — members filter bar

- [x] 5.1 Add a filter bar component on the members list page with `<FulltextSearchInput />` and — for MEMBERS:MANAGE users — a pill group "Aktivní / Neaktivní / Vše".
- [x] 5.2 Wire URL query params as the single source of truth; on first render, inject the default `status=ACTIVE` via `setSearchParams(..., { replace: true })`. Back/forward and reload preserve state.
- [x] 5.3 Hide the status pill group when the current user does not have MEMBERS:MANAGE (derive from the root resource's authorities / `CurrentUserData`).
- [x] 5.4 Empty-state message when the filtered result is empty ("Žádní členové neodpovídají zadaným filtrům.").
- [x] 5.5 Localize filter-bar labels in `src/localization/labels.ts`.
- [x] 5.6 Remove the per-row active-status column from the members table component. Action affordances (suspend, resume, permissions link) stay in the action column. Update any component snapshots / tests that asserted the column's presence.

## 6. Frontend — tests & verification

- [x] 6.1 Component tests: members filter bar renders controls; hides the status pill group for non-MANAGE users.
- [x] 6.2 Integration-style test: changing the status pill updates the URL and the request's `status` param.
- [x] 6.3 Manual verification on `http://localhost:3000` (`ZBM9500` regular member, `ZBM9000` admin): fulltext across first name / last name / registration number, case and diacritics insensitivity, multi-word AND, status pill switching (admin only), default "Aktivní" on first load, URL preservation on reload + back/forward, empty state.

## 7. Documentation

- [ ] 7.1 Create `docs/frontend-patterns.html` capturing the filter-bar patterns: pill group (3-valued exclusive selector), fulltext search (debounce/min-chars/URL/shared component), binary toggle (authority-gated checkbox), filter bar layout, default-filter injection pattern, empty-state copy template. Short, living document; expect future additions.
- [ ] 7.2 Run `openspec validate members-list-filter --strict`.

## 8. Archive

- [ ] 8.1 On merge, archive `members-list-filter` via OpenSpec archive workflow.
