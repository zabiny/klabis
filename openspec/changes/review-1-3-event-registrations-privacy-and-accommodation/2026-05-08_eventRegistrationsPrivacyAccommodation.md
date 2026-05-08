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

