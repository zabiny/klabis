# Team Coordination File: review-1-3 — Event Registrations Privacy & Accommodation List

**Started:** 2026-05-08
**Proposal:** `openspec/changes/review-1-3-event-registrations-privacy-and-accommodation/`

## Goal

Implement three review notes (N9, N10, N11) sharing one authorization principle "event coordinator OR EVENTS:REGISTRATIONS":
- **N9** — Hide `registrationTime` for non-authorized users (field-level via `@OwnerVisible` + `@OwnerId`)
- **N10** — Sortable headers in registration list (firstName, lastName, category, + registrationTime for authorized)
- **N11** — Accommodation list endpoint + print-friendly frontend page

## Iteration Plan (vertical slices)

1. **Iteration 1 (N9)** — Verify and use `@OwnerVisible`/`@OwnerId` for `registrationTime` in `EventRegistrationSummaryDto`. Populate `eventCoordinatorId` from surrounding event. Backend tests + FE table column hiding.
2. **Iteration 2 (N10)** — Sortable backend endpoint + FE sortable headers. Silent fallback for unauthorized `sort=registrationTime` and unknown sort fields.
3. **Iteration 3 (N11 backend)** — `GET /api/events/{eventId}/accommodation-list` endpoint with method-level auth (coordinator OR EVENTS:REGISTRATIONS). HAL affordance on event detail.
4. **Iteration 4 (N11 frontend)** — New route `/events/:id/accommodation-list`, print-friendly layout, action in event detail action bar.
5. **Final** — Simplify review, code review, tests, commit.

## Workspace

- backend module: `backend/src/main/java/.../events/registrations/`
- frontend: `frontend/src/...` (registration list table + event detail)

## Log

### Iteration 1 — 2026-05-08 (backend, tasks 1.1–1.4)

**What changed:**
- `RegistrationSummaryDto` — renamed field `registeredAt` → `registrationTime`, added `@OwnerVisible @HasAuthority(EVENTS_REGISTRATIONS)` on it, added new `@OwnerId MemberId eventCoordinatorId` sibling. Added `@JsonInclude(NON_NULL)` and class-level `@HandleAuthorizationDenied(NullDeniedHandler)` so hidden fields disappear from JSON.
- `RegistrationDtoMapper` — both `toDto` and `toDtoList` now accept an `Event` parameter; `eventCoordinatorId` is populated from `event.getEventCoordinatorId()` on every row.
- `EventRegistrationController` — `buildRegistrationItems` passes the loaded `event` to the mapper (no API change).
- `EventController` — `buildRegistrationDtos` signature changed to accept `Event` (already loaded); calls updated accordingly.
- `EventRegistrationE2ETest` — updated assertion at list endpoint: `EVENTS_MANAGE`-only user now expects `registrationTime` absent (was `registeredAt` present — reflected the old behavior that violated the new spec).
- `EventRegistrationControllerTest` — added new `RegistrationTimePrivacyTests` nested class with 3 tests: regular member hidden, coordinator visible, `EVENTS_REGISTRATIONS` visible.

**Gaps found in Task 1.1:** None. `FieldSecurityBeanSerializerModifier` + `SecuredBeanPropertyWriter` fully support `@OwnerVisible`/`@OwnerId` on response DTO record components. `MemberId` is already registered as UUID-convertible via `MemberIdToUuidConverter`. No changes to `common/security/fieldsecurity/`.

**Test results:** 71/71 passed (EventRegistrationControllerTest 32/32, EventControllerTest + EventRegistrationE2ETest).

### Iteration 1 — 2026-05-08 (frontend, task 1.5)

**What changed:**
- `EventDetailPage.tsx` — `RegistrationData` interface: `registeredAt: string` → `registrationTime?: string` (optional, matches backend's conditional omission).
- `RegistrationsTable` component: `<TableCell column="registeredAt">` → `<TableCell column="registrationTime">` with same `formatDateTime` render. Added `hideEmptyColumns` prop to `HalEmbeddedTable` — when backend omits `registrationTime` for non-authorized users, no row has the field and `KlabisTable` auto-hides the column. Added `alwaysVisible` to the `_actions` column so it is never auto-hidden by `hideEmptyColumns`.
- `EventDetailPage.test.tsx` — updated existing column test (removed assertion that timestamp column always appears); added `buildRegistrationRowWithoutTimestamp` helper; added `registrationTime column visibility (N9 privacy)` describe with 3 tests: column hidden when no rows have `registrationTime`, shown when all rows have it, shown when at least one row has it.

**OpenAPI regen:** Not required — `registrationTime` was already present in the backend DTO from task 1.2; `registeredAt` was the old frontend-only name (never in the OpenAPI spec). No `klabisApi.d.ts` regeneration needed.

**Test results:** 1261/1261 passed (all frontend tests).

### Iteration 2 — 2026-05-08 (backend, tasks 2.1–2.3, 2.5)

**What changed:**
- `EventRegistrationController.listRegistrations` — added `@RequestParam(required = false) String sort` parameter. Reads auth from `SecurityContextHolder` and checks whether caller is authorized for `registrationTime` sort (has `EVENTS:REGISTRATIONS` OR matches `event.getEventCoordinatorId()`). Delegates to `RegistrationSortApplier.sort()`. Updated self-link call to pass `null` for sort.
- `RegistrationSortApplier` (new class) — stateless helper that applies in-memory sorting to `List<EventRegistration>`. Supported fields: `firstName`, `lastName`, `category`, `registrationTime`. Parses `sort` param as `fieldName` or `fieldName,desc`. Unknown fields and unauthorized `registrationTime` silently fall back to `registrationTime ASC`. Case-insensitive string comparison for name/category fields.
- `EventController` (postprocessor) — updated `listRegistrations` link reference to include `null` for new sort param.
- `EventRegistrationControllerTest` — added `RegistrationSortTests` nested class with 7 tests covering: default sort, `firstName`, `lastName`, `category`, `registrationTime` authorized, `registrationTime` unauthorized (fallback), unknown field (fallback).

**Design choices:**
- Sort format: `fieldName` or `fieldName,desc` — consistent with Spring's Pageable sort convention used in `EventController`.
- Authorization check reuses `EventAffordanceSupport.hasAuthority()` and `resolveMemberId()` — same helpers used elsewhere in the module.
- `RegistrationSortApplier` is package-private and tested indirectly via controller tests.

**Test results:** 39/39 passed (EventRegistrationControllerTest); 59/59 passed (EventControllerTest).

### Iteration 2 — 2026-05-08 (frontend, task 2.4)

**What changed:**
- `EventDetailPage.tsx` — `RegistrationsTable` component: added `sortable` prop to `firstName`, `lastName`, `category`, and `registrationTime` columns; added `defaultOrderBy="registrationTime"` to `HalEmbeddedTable` so the initial sort state matches the server-side default (FCFS). The `hideEmptyColumns` mechanism already handles `registrationTime` sort button visibility — when the column is hidden (no rows have the field), the sort button is absent automatically.
- `EventDetailPage.test.tsx` — added `sortable column headers (N10)` describe with 6 tests: `firstName` sortable, `lastName` sortable, `category` sortable when event has categories (row must include `category` value for `hideEmptyColumns` to keep the column), `registrationTime` sortable when visible, `registrationTime` sort button absent when hidden (non-authorized user), clicking `firstName` sort sends `sort=firstName%2Casc` in the URL.

**Design notes:**
- `KlabisTableWithQuery` already encodes `sort=${field},${direction}` into the URL; the backend accepts `fieldName,asc` and `fieldName,desc` — `asc` suffix is not recognized as `desc` so it falls through to ASC behavior, matching the expected behavior.
- No new component abstractions needed — the existing `sortable` prop on `TableCell` and the `handleSort` + `queryUrl` mechanism in `KlabisTableWithQuery` cover the full requirement.
- `registrationTime` sort is tied to column visibility via `hideEmptyColumns`: when backend omits the field (non-authorized user), the column is hidden, and so is its sort button.

**Test results:** 1267/1267 passed (all frontend tests).

### Iteration 3 — 2026-05-08 (backend, tasks 3.1, 3.2, 3.3, 3.6)

**What changed:**
- `Members` interface — added `findAccommodationDataByIds(Collection<MemberId>)` returning `Map<MemberId, MemberAccommodationDto>`.
- `MemberAccommodationDto` (new, `members` root package) — cross-module read DTO with: `memberId`, `firstName`, `lastName`, `identityCardNumber`, `identityCardValidityDate`, `dateOfBirth`, `addressStreet`, `addressCity`, `addressPostalCode`, `addressCountry`. All fields nullable.
- `MembersImpl` — added `findAccommodationDataByIds` implementation using `memberRepository.findAllByIds`; maps `Member.getIdentityCard()` and `Member.getAddress()` defensively (null-safe).
- `AccommodationListItemDto` (new, `events.infrastructure.restapi`) — package-private record with `@Relation(collectionRelation="accommodationList")` and `@JsonInclude(NON_NULL)`.
- `EventRegistrationController` — added `GET /api/events/{eventId}/registrations/accommodation-list` endpoint; authorization check via `isAuthorizedForAccommodationList()` using same `EventAffordanceSupport` helpers as the sort authorization check (no new annotation, no SpEL bean reference). Throws `AccessDeniedException` → 403 for unauthorized callers.
- `EventDetailsPostprocessor` — added `accommodation-list` link to event detail response when caller is coordinator OR has `EVENTS:REGISTRATIONS`.
- `EventRegistrationControllerTest` — added `AccommodationListTests` nested class with 5 tests: coordinator 200, EVENTS:REGISTRATIONS 200, unauthorized 403, null identityCard fields absent in JSON, null address fields absent in JSON.
- `EventControllerTest` — added 3 tests in `GetEventTests`: accommodation-list link present for EVENTS:REGISTRATIONS user, link present for coordinator, link absent for regular member.

**Design choices:**
- No new annotation or SpEL `@PreAuthorize` with bean reference. The authorization logic is inline in the controller method using `isAuthorizedForAccommodationList()` — identical pattern to `isAuthorizedForRegistrationTimeSort()` added in iteration 2.
- `MemberAccommodationDto` exposes address as flat fields (not nested object) to avoid exposing domain `Address` type cross-module. Frontend renders address from flat fields.
- `@JsonInclude(NON_NULL)` on `AccommodationListItemDto` ensures missing fields (null identityCard, null address) are absent from JSON — frontend's "neuvedeno" fallback is handled by absence check.

**Test results:** 2361/2361 passed (all backend tests).

**URL correction (post-iteration fix):** URL corrected to match proposal: `/api/events/{eventId}/accommodation-list`. Endpoint moved from `EventRegistrationController` (where it resolved to `/api/events/{eventId}/registrations/accommodation-list`) to `EventController` (direct `@GetMapping("/{eventId}/accommodation-list")`). `EventDetailsPostprocessor` link updated to use `methodOn(EventController.class).getAccommodationList(...)`. AccommodationListTests moved from `EventRegistrationControllerTest` to `EventControllerTest`. 106/106 tests pass.

### Iteration 4 — 2026-05-08 (frontend, tasks 3.4, 3.5)

**What changed:**
- `src/localization/labels.ts` — added: `buttons.print`, `buttons.accommodationList`, `sections.accommodationList`, `ui.notProvided`, `tables.identityCardNumber`, `tables.identityCardValidityDate`, `tables.dateOfBirth`, `tables.address`.
- `AccommodationListPage.tsx` (new, `pages/events/`) — standalone route component:
  - Uses `useParams` to get event ID, fetches `/api/events/{id}` via `useAuthorizedQuery` to get the HAL `accommodation-list` link, then fetches the accommodation list from that link.
  - Renders a plain HTML `<table>` with columns: jméno, příjmení, číslo OP, platnost OP, datum narození, adresa.
  - Formats dates via existing `formatDate` (Czech locale `cs-CZ`).
  - Renders `labels.ui.notProvided` ("neuvedeno") for null/absent fields.
  - Renders `labels.buttons.print` ("Tisknout") button that calls `window.print()`.
  - Back link to `/events/{id}`.
  - Print-friendly styling via Tailwind `print:` variants (text/bg forced to black/white).
- `AccommodationListPage.test.tsx` (new) — 17 tests covering: loading/error states, page structure (heading, event name, back link, Tisknout button), all 6 column headers, data rendering (names, idCard, dates, address), "neuvedeno" fallback for each missing field group, multiple rows, empty table.
- `App.tsx` — added route `/events/:id/accommodation-list` → `AccommodationListPage`.
- `EventDetailPage.tsx` — in the action bar section: when `resourceData._links?.['accommodation-list']` is present, renders a `Link` to `${route.pathname}/accommodation-list` with label "Seznam pro ubytování" (from `labels.buttons.accommodationList`). Used `List` icon from `lucide-react`.
- `EventDetailPage.test.tsx` — added `accommodation list action (N11 task 3.5)` describe with 3 tests: link shown when HAL link present, not shown when absent, href points to correct frontend route.
- `index.css` — added `@media print` block: hides nav/header/aside/bottom-nav, enforces `thead { display: table-header-group }` for repeating headers on print pages, forces black-on-white text.

**Design choices:**
- `AccommodationListPage` does not use `useHalPageData` because the route `/events/:id/accommodation-list` would map to a non-existent API path `/api/events/:id/accommodation-list`. Instead, it constructs the event URL from `useParams` and fetches it directly via `useAuthorizedQuery`.
- Navigation from EventDetailPage uses `route.pathname + '/accommodation-list'` (frontend route) rather than following the HAL link href (which points to the API endpoint).
- Address rendered as a single cell with flat fields joined by ", " — mirrors the flat-field model from `AccommodationListItemDto`.

**Test results:** 1287/1287 passed (all frontend tests).

### Code review fixes — 2026-05-08 (HIGH priority findings H1, H2, H3)

**H1 — Deduplicated "coordinator OR EVENTS:REGISTRATIONS" authorization logic.**
Added `EventAffordanceSupport.isCoordinatorOrHasRegistrationsAuthority(Authentication, Event)` static helper. Replaced three identical inline checks:
- `EventRegistrationController.isAuthorizedForRegistrationTimeSort()` — private method removed; call site updated.
- `EventController.isAuthorizedForAccommodationList()` — private method removed; call site updated.
- `EventDetailsPostprocessor.process()` inline — replaced with single helper call.

**H2 — `eventCoordinatorId` excluded from JSON output.**
Added `@JsonIgnore` on `RegistrationSummaryDto.eventCoordinatorId` record component. The field-security advice reads record components directly (not via Jackson), so `@OwnerId` resolution for `@OwnerVisible` on `registrationTime` continues to work. Verified by `RegistrationTimePrivacyTests` — 118/118 passed.

**H3 — Removed unused `memberId` field from `MemberAccommodationDto`.**
Removed `UUID memberId` component and its `UUID` import from the record. Updated `MembersImpl.fromMemberToAccommodationDto` to omit the `member.getId().uuid()` argument. Updated 4 `MemberAccommodationDto` constructor calls in `EventControllerTest` (accommodation-list test fixtures).

**Test results:** 118/118 passed (EventRegistrationControllerTest + EventControllerTest + EventRegistrationE2ETest).

### Bugfix: templated links — 2026-05-08

**Problem:** After adding `@RequestParam(required = false) String sort` to `listRegistrations` in iteration 2, all `klabisLinkTo(methodOn(EventRegistrationController.class).listRegistrations(eventId, null))` call sites produced links with an unexpanded URI template suffix `{?sort}`. The frontend `HalEmbeddedTable` used the literal URL including the template syntax, causing HTTP 400.

**Fix:** Called `.expand()` on the resulting `Link` at both affected sites:
- `EventRegistrationController.listRegistrations` — self-link on the registration collection response.
- `EventDetailsPostprocessor.process()` — `registrations` link on the event detail response.

`getAccommodationList` has no `@RequestParam` parameters so its link was unaffected.

**Tests added:**
- `EventRegistrationControllerTest.ListRegistrationsTests.selfLinkHrefMustNotContainUriTemplateSyntax` — asserts `$._links.self.href` does not contain `{?sort}`.
- `EventControllerTest.GetEventTests.registrationsLinkHrefMustNotContainUriTemplateSyntax` — asserts `$._links.registrations.href` does not contain `{?sort}`.

**Test results:** 108/108 passed (EventRegistrationControllerTest + EventControllerTest).

