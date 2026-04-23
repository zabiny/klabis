## 1. Backend foundation — domain & persistence for fulltext

- [ ] 1.1 Write failing repository tests for fulltext filter on event `name` (case-insensitive, diacritics-insensitive).
- [ ] 1.2 Extend `EventFilter` value object with `fulltextQuery` field and a `withFulltext(String)` factory/wither.
- [ ] 1.3 Ensure PostgreSQL `unaccent` extension is available (Flyway migration `CREATE EXTENSION IF NOT EXISTS unaccent`); add H2 alias/function so tests on H2 pass.
- [ ] 1.4 Extend `EventRepositoryAdapter` JDBC query with unaccent-based `LIKE` against `name` + `location`, AND-ing tokens split on whitespace, each token OR'd across both columns.
- [ ] 1.5 Green: tests for name and location matches pass.
- [ ] 1.6 Add tests for multi-word AND semantics; make them pass.
- [ ] 1.7 Refactor SQL composition to keep the repository adapter readable.

## 2. Backend foundation — domain & persistence for registeredBy

- [ ] 2.1 Write failing repository tests: given an event with and without a registration for a given member, filter by `registeredBy` returns only the one with a matching registration.
- [ ] 2.2 Extend `EventFilter` with `registeredBy: MemberId` field.
- [ ] 2.3 Extend `EventRepositoryAdapter` JDBC query with `EXISTS (SELECT 1 FROM event_registrations er WHERE er.event_id = e.id AND er.member_id = :registeredBy)` clause, applied only when `registeredBy` is set.
- [ ] 2.4 Add tests asserting the filter includes CANCELLED and FINISHED events with a matching registration; make them pass.
- [ ] 2.5 Refactor.

## 3. Backend — REST controller wiring

- [ ] 3.1 Write failing `@WebMvcTest` for `EventController.listEvents` accepting `q` parameter and passing it to the service as `EventFilter.fulltextQuery`.
- [ ] 3.2 Add `@RequestParam(required = false) String q` to `EventController.listEvents`; map into `EventFilter`.
- [ ] 3.3 Write failing test for `organizer` and `coordinator` params (spec lists them but controller doesn't wire them today).
- [ ] 3.4 Add `@RequestParam` for `organizer` and `coordinator`; map into `EventFilter`.
- [ ] 3.5 Write failing test for `registeredBy=me` → resolved to current user's `MemberId`.
- [ ] 3.6 Add `@RequestParam String registeredBy`; in the controller, translate `"me"` to the acting user's member id. Reject any other value with 400.
- [ ] 3.7 Write test: user without a member profile calling `GET /api/events?registeredBy=me` returns 200 with empty page (tied to the spec scenario). Make it pass.
- [ ] 3.8 Refactor controller to keep the listing method readable.

## 4. Frontend — filter bar component

- [ ] 4.1 Add a filter bar component to the events list page with fulltext input, time window pill group (Budoucí / Proběhlé / Vše), and a "Moje přihlášky" checkbox.
- [ ] 4.2 Wire URL query params as the single source of truth; back/forward and reload preserve state.
- [ ] 4.3 Hide the "Moje přihlášky" checkbox when the current user has no member profile (derive from `CurrentUserData.memberId`).
- [ ] 4.4 Implement widget → date param translation: Budoucí maps to `dateFrom=today`, Proběhlé maps to `dateTo=today-1`, Vše adds neither. Unit-test the mapping.
- [ ] 4.5 Implement default sort per widget: Budoucí → `sort=eventDate,asc`, Proběhlé/Vše → `sort=eventDate,desc`. User-clicked column sort overrides this default.
- [ ] 4.6 Debounce fulltext input at ~250 ms; ignore inputs shorter than 2 characters after trim.
- [ ] 4.7 Empty-state message when the filtered result is empty.
- [ ] 4.8 Localize filter-bar labels in `src/localization/labels.ts`.

## 5. Frontend — tests & verification

- [ ] 5.1 Component tests: filter bar renders controls; hides "Moje" toggle for users without member profile.
- [ ] 5.2 Integration-style test: changing time window updates the URL and the request's date params.
- [ ] 5.3 Manual verification on `http://localhost:3000` (login as `ZBM9500` club member): fulltext, time window default + switch, "Moje" toggle, combination filters, empty state, sort behavior.

## 6. Documentation & follow-ups

- [ ] 6.1 Update `openspec/changes/gh-88-my-registrations-filter/proposal.md`: add a section "Already delivered in gh-88-events-list-filters" listing the "Moje přihlášky" filter on the events list, and narrow remaining scope to the follow-up items (e.g., dashboard widget, "Další závody" widget).
- [ ] 6.2 Create a task-queue item (via `openspec-ext:task-queue` skill or manual file in `tasks/`) for the HAL-Forms search-affordance foundation: introduce a shared pattern for GET list endpoints to expose their filter dimensions via HAL-Forms templates, and migrate events list as the first consumer.
- [ ] 6.3 Run `openspec validate gh-88-events-list-filters --strict` to confirm the proposal is still consistent with archived specs.

## 7. Archive

- [ ] 7.1 On merge, archive `gh-88-events-list-filters` via OpenSpec archive workflow.
- [ ] 7.2 Leave GitHub issue #88 OPEN and leave `gh-88-my-registrations-filter` proposal OPEN — both continue to track the broader scope beyond this change.
