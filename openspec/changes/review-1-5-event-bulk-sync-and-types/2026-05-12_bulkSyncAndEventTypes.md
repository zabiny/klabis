# TCF — review-1-5: Bulk ORIS sync + Event Types

This Team Coordination File tracks progress for implementation of the `review-1-5-event-bulk-sync-and-types` proposal.

**Read this file before starting work.** Append a concise summary of your changes and any issues at the bottom when you finish.

## Proposal scope (high level)

- **Phase A (N5)** — Bulk ORIS sync: backend endpoint `POST /api/events/sync-from-oris/all-upcoming` (sync, sequential, partial-failure tolerant, aggregate response) + frontend toolbar action with progress modal and summary.
- **Phase B (N13)** — Event Types catalog: new `event-types` capability (CRUD aggregate + REST + admin UI) + optional `Event.eventType` field + ORIS auto-mapping by name (case-insensitive) + table column + filter.

Reference proposal docs:
- `proposal.md`
- `design.md`
- `tasks.md`
- `specs/events/spec.md`
- `specs/event-types/spec.md`

## Plan

Vertical slices, sequential (Phase A is fully independent and can ship first; Phase B is split into BE-then-FE iterations).

| Iter | Scope | Agent | Status |
|------|-------|-------|--------|
| 1 | A1 — Backend bulk-sync service + endpoint + HAL affordance + integration tests | backend-developer | TODO |
| 2 | A2 — Frontend toolbar action, progress modal, summary view | frontend-developer | TODO |
| 3 | B1+B2+B3 — DB migration, `EventType` aggregate, persistence, REST CRUD controller + tests | backend-developer | DONE |
| 4 | B4 — Wire `eventType` into `Event` aggregate + REST DTO/forms + tests | backend-developer | DONE |
| 5 | B5 — ORIS import auto-mapping helper + tests | backend-developer | DONE |
| 6 | B6 — Frontend admin page for event types | frontend-developer | DONE |
| 7 | B7 — Frontend event form/table column/filter/detail badge + labels | frontend-developer | DONE |
| 8 | Simplify pass + code review + fixes + docs (task 9) | mix | TODO |

After every iteration: tests pass → commit. E2E verification (A3, B8) is optional manual step at the end.

## Notes for subagents

- Always run tests via `developer:test-runner-skill` (do NOT run gradlew/npm directly in parallel).
- Use `backend-patterns` skill for all backend code; use `hal-navigator-patterns` for frontend.
- Mark completed tasks in `tasks.md` as you finish them.
- Do NOT bypass commit signing — use `--no-gpg-sign` only if 1Password agent fails.
- Conventional Commits — scope: `events` for N5, `event-types` (or `events`) for N13.

## Open implementation decisions

- ORIS field for auto-mapping: backend-developer will pick Level or Discipline during iter 5 and document in `developerManual`.
- DB constraint for unique name: implement as `UNIQUE INDEX ON event_types (LOWER(name))` in V001.

## Progress log

(subagents append below)

---

### Iter 3 — B1–B3 EventType aggregate + persistence + REST CRUD (2026-05-12)

Tasks B1.1–B3.3 completed. 49/49 new tests pass. Full suite 2504/2505 (1 pre-existing failure in EventManagementE2ETest unrelated to this iteration).

**Analogue patterned after:** `CategoryPreset` capability (`com.klabis.events.domain.CategoryPreset` + memento + controller).

**Key decisions:**
- `UNIQUE INDEX ON event_types (LOWER(name))` replaced with plain `UNIQUE INDEX ON name` for H2 test compatibility. Case-insensitive uniqueness enforced at application layer via `existsByNameIgnoreCase`. Production can add functional index via future migration.
- `event_types` table placed before `events` in V001 (FK dependency order).
- `EventTypeInUseException` loads up to 5 affected event names for the error message.
- `existsEventReferencingType` + `findEventNamesReferencingType` queries run against `events.event_type_id` column (no Event aggregate changes needed this iteration).

**Changed/created files:**
- `V001__initial_schema.sql` — added `event_types` table + `event_type_id` FK on `events`
- `EventTypeId.java` — value object in `com.klabis.events` root
- `eventtype/domain/EventType.java` — aggregate with CreateEventType/UpdateEventType commands
- `eventtype/domain/EventTypeRepository.java` — port interface
- `eventtype/domain/EventTypeNotFoundException.java`, `EventTypeNameAlreadyExistsException.java`, `EventTypeInUseException.java`
- `eventtype/infrastructure/jdbc/EventTypeMemento.java`, `EventTypeJdbcRepository.java`, `EventTypeRepositoryAdapter.java`
- `eventtype/application/EventTypeManagementPort.java`, `EventTypeManagementService.java`
- `eventtype/infrastructure/restapi/EventTypeController.java`, `EventTypeDto.java`, `EventTypeIdMixin.java`, `EventTypeDtoMapper.java`, `EventTypeExceptionHandler.java`
- Tests: `EventTypeTest.java` (22 domain tests), `EventTypeRepositoryAdapterTest.java` (12 persistence tests), `EventTypeControllerTest.java` (15 controller tests)

---

### Iter 6 — B6 Frontend admin page for event types (2026-05-12)

Tasks B6.1–B6.5 completed. 15 new tests pass. Full suite 1407/1407.

**Pattern:** Follows `CategoryPresetsPage` exactly — `HalEmbeddedTable` + per-row action buttons (edit/delete) open `HalFormDisplay` inside `Modal`. Create button via `HalFormButton`. 409 conflict errors rendered automatically by `HalFormDisplay` error display.

**Key decisions:**
- `event-types` added to `ADMIN_RELS` in `useRootNavigation.ts` — nav link classified as admin section item (visible only when backend returns it, i.e. user has EVENTS:MANAGE authority).
- Color swatch rendered as `<span>` with `style.backgroundColor` and `title` attribute showing hex value.
- `defaultOrderBy="sortOrder"` on `HalEmbeddedTable` to match backend sort semantics.

**BLOCKED — backend change required:**
- `EventTypeController.java` needs a `EventTypesRootPostprocessor` (inner class) that adds `klabisLinkTo(methodOn(EventTypeController.class).listEventTypes()).ifPresent(link -> model.add(link.withRel("event-types")))` to `EntityModel<RootModel>`. Without this, the nav link is never returned in `/api` response and "Typy akcí" never appears in the sidebar. Pattern to follow: `EventsRootPostprocessor` in `EventController.java` (lines ~625–635). This is a backend change — outside frontend-developer scope.

**Changed/created files:**
- `frontend/src/localization/labels.ts` — added `nav['event-types']`, `templates.createEventType/updateEventType/deleteEventType`, `sections.eventTypesList/eventTypesListHeading`, `fields.color/sortOrder/eventTypeId`
- `frontend/src/hooks/useRootNavigation.ts` — added `'event-types'` to `ADMIN_RELS`
- `frontend/src/pages/Layout.tsx` — added `ListChecks` icon for `event-types` rel
- `frontend/src/App.tsx` — added `/event-types` route → `EventTypesPage`
- `frontend/src/pages/events/EventTypesPage.tsx` — new admin page (list + create/edit/delete modals)
- `frontend/src/pages/events/EventTypesPage.test.tsx` — 15 tests

---

### Iter 2 — Frontend bulk sync toolbar + modal (2026-05-12)

Implemented tasks A2.1–A2.4. All 1392 frontend tests pass.

**Changed files:**
- `frontend/src/localization/labels.ts` — added `templates.syncAllUpcomingFromOris`, `dialogTitles.syncAllUpcomingFromOris`, `bulkSync.*` labels
- `frontend/src/components/events/BulkSyncOrisModal.tsx` — new component: triggers mutation on mount, shows spinner during `isPending`, switches to summary with success/failure counts + failure list on `isSuccess`
- `frontend/src/pages/events/EventsPage.tsx` — wires `syncAllUpcomingFromOris` template to toolbar button + `BulkSyncOrisModal`; invokes `route.refetch` on sync complete
- `frontend/src/components/events/BulkSyncOrisModal.test.tsx` — 6 new tests (progress state, summary state, onClose, mutation trigger)
- `frontend/src/pages/events/EventsPage.test.tsx` — 3 new tests (button visible/absent, modal opens on click) + `BulkSyncOrisModal` mock

**HAL template name:** `syncAllUpcomingFromOris` (derived from method name by Spring HATEOAS)
**No blocked items.**

---

### Iter 5 — B5 ORIS import auto-mapping (2026-05-12)

Tasks B5.1–B5.4 completed. 7 new tests pass. Full suite 2517/2518 (1 pre-existing failure in EventManagementE2ETest, unchanged).

**Chosen ORIS field: `Level.nameCZ`**
Level represents competition level (e.g., "Klub", "Oblastní přebor", "Mistrovství ČR") — maps naturally to Klabis event type categories. Discipline encodes sport variant (OB/MTBO/LOB), not event type.

**Preserve behavior on no match:** During `syncEventFromOris`, when ORIS Level is null or has no catalog match the existing `eventTypeId` is preserved (not cleared). Rationale: manager may have manually set the type; ORIS doesn't know about the Klabis catalog. During initial `importEventFromOris`, no existing type exists so it simply remains empty.

**Implementation approach: inline into `OrisEventImportService`** (no separate class). The lookup is a single null-safe helper `resolveEventTypeFromOrisLevel(Level)`. A dedicated `EventTypeAutoMapper` wrapper would be needless indirection. New `Event.applyAutoMappedEventType(EventTypeId)` domain method applies the resolved type; passing null is a no-op (preserves existing type).

**Changed files:**
- `domain/Event.java` — added `applyAutoMappedEventType(EventTypeId)` domain method
- `application/OrisEventImportService.java` — added `EventTypeRepository` dependency; `resolveEventTypeFromOrisLevel()` private helper; both `importEventFromOris` and `syncEventFromOris` call `applyAutoMappedEventType` after the main sync
- Tests: `OrisEventTypeAutoMappingTest.java` (7 tests: import match, import no match, import null level, import case-insensitive, sync match, sync preserve on no match, sync preserve on null level); updated `OrisEventImportServiceTest.java` constructor call to pass mock `EventTypeRepository`

---

### Iter 4 — B4 Wire eventTypeId into Event aggregate + REST + tests (2026-05-12)

Tasks B4.1–B4.4 completed. 12/13 new E2E tests pass; 1 pre-existing failure (`shouldCreateEventWithoutLocation`) unchanged.

**Key design decisions:**
- REST representation: `eventTypeId` exposed as raw UUID field (same pattern as `eventCoordinatorId`). No embedded resolved object (name/color) in response — consumers follow the HAL link `event-type` → `GET /api/event-types/{id}` to resolve details.
- `EventTypeController.getEventType` and class made `public` to allow cross-package `methodOn()` reference from `EventController` postprocessors.
- `@JsonInclude(NON_NULL)` on `EventDto`/`EventSummaryDto` ensures absent `eventTypeId` is omitted from JSON.
- `EventTypeIdMixin` (`@JsonValue`/`@JsonCreator`) handles UUID serialization — `eventTypeId` appears as plain UUID string in API responses.

**Changed files:**
- `domain/Event.java` — `@Association EventTypeId eventTypeId` field; `CreateEvent`/`UpdateEvent` gain `eventTypeId`; `reconstruct()` 15 params; `getEventTypeId()` returns `Optional<EventTypeId>`
- `infrastructure/jdbc/EventMemento.java` — `event_type_id` UUID mapping added
- `infrastructure/restapi/EventDto.java`, `EventSummaryDto.java` — `EventTypeId eventTypeId` component
- `infrastructure/restapi/EventDtoMapper.java`, `CreateEventRequest.java`, `UpdateEventRequest.java` — eventTypeId wired through
- `infrastructure/restapi/EventController.java` — passes eventTypeId to commands; postprocessors add `event-type` HAL link
- `infrastructure/bootstrap/EventsDataBootstrap.java` — 27 `CreateEvent` calls updated with `null` eventTypeId
- `eventtype/infrastructure/restapi/EventTypeController.java` — class and `getEventType` made `public`
- Tests: `EventAssert.java`, `EventJdbcRepositoryTest.java` (2 persistence tests), `EventManagementE2ETest.java` (4 E2E tests), `EventTestDataBuilder.java`; fixed `Event.reconstruct` call sites in calendar/registration tests

---

### Iter 6 (backend complement) — EventTypesRootPostprocessor (2026-05-12)

Added `EventTypesRootPostprocessor` at the bottom of `EventTypeController.java` — a `@MvcComponent` that implements `RepresentationModelProcessor<EntityModel<RootModel>>` and appends an `event-types` HAL link via `klabisLinkTo(methodOn(EventTypeController.class).listEventTypes())`, which is authority-gated by the existing `@HasAuthority(Authority.EVENTS_MANAGE)` on `listEventTypes()`. Also made `listEventTypes()` `public` (was package-private) so the cross-package `methodOn(...)` invocation compiles. 1 new unit test passes; `EventTypeControllerTest` 18/18 unchanged.

**Changed files:**
- `backend/src/main/java/com/klabis/events/eventtype/infrastructure/restapi/EventTypeController.java` — `listEventTypes()` made `public`; `EventTypesRootPostprocessor` added at end of file
- `backend/src/test/java/com/klabis/events/eventtype/infrastructure/restapi/EventTypesRootPostprocessorTest.java` — new unit test

---

### Iter 7 — B7 Frontend event form/table column/detail badge + labels (2026-05-12)

Tasks B7.1, B7.2, B7.3, B7.5 completed. **B7.4 BLOCKED** — backend filter support missing.
21 new tests added. Full suite 1428/1428.

**B7.1 — Event form type dropdown:** `EventTypeSelectField` component wraps `SelectField` with options loaded from `useEventTypes` hook (cached, 5 min staleTime). `eventFormFieldsFactory` intercepts `eventTypeId` field by name before delegating to `klabisFieldsFactory`. First option is "—" (empty = not set).

**B7.2 — Events table "Typ" column:** Added `eventTypeId` column using `EventTypeBadge` + `getById` lookup from `useEventTypes`. Column is hidden via existing `hideEmptyColumns` when no events have a type assigned.

**B7.3 — Event detail badge:** `EventTypeBadge` shown inline with the status badge in the header. Also wired `eventTypeId` into the edit form row so it appears when editing.

**B7.4 BLOCKED:** Backend `EventController.listEvents` has no `eventTypeId` `@RequestParam`. Required backend change: add `@RequestParam(required = false) UUID eventTypeId` to `listEvents`, extend `EventFilter` with `withEventTypeId`, add JDBC predicate in `EventJdbcRepository`, and update HAL `self` link in `listEvents`.

**B7.5 — Labels:** Added `tables.eventType` = "Typ" and `eventsFilter.eventTypeSelectPlaceholder` = "— bez filtru —" to `labels.ts`.

**New/changed files:**
- `frontend/src/hooks/useEventTypes.ts` — new hook (6 tests)
- `frontend/src/components/events/EventTypeBadge.tsx` — new component (3 tests)
- `frontend/src/components/events/EventTypeSelectField.tsx` — new Formik select field
- `frontend/src/components/events/eventFormFieldsFactory.tsx` — intercept `eventTypeId` (4 tests)
- `frontend/src/pages/events/EventsPage.tsx` — Typ column + useEventTypes (4 new tests)
- `frontend/src/pages/events/EventDetailPage.tsx` — type badge in header + edit row (4 new tests)
- `frontend/src/localization/labels.ts` — tables.eventType, eventsFilter.eventTypeSelectPlaceholder

---

### B7.4 backend — multi-value eventTypeId filter on listEvents (2026-05-12)

Task B7.4 unblocked. Added multi-value `?eventTypeId=x&eventTypeId=y` filter to `GET /api/events`. Spring binds `List<UUID>` automatically; the controller converts to `List<EventTypeId>` and delegates to a new `EventFilter.withEventTypeIds(List<EventTypeId>)` fluent method. The JDBC adapter applies `Criteria.where("event_type_id").in(...)` only when the list is non-empty, so absent param = no restriction. The `self` link round-trips the list value via the updated `methodOn(EventController.class).listEvents(...)` call. Also fixed one direct `EventFilter` constructor call in `EventFilterTest` (needed an extra null arg for the new field). 4 new integration tests in `EventJdbcRepositoryTest.FilterByEventTypeIds`; all 56 repository tests + 82 controller tests + 4 E2E filter tests pass (142 total, 1 pre-existing failure in `EventManagementE2ETest` unchanged).

**Changed files:**
- `backend/src/main/java/com/klabis/events/domain/EventFilter.java` — new `eventTypeIds` field + `withEventTypeIds` fluent method; all constructors updated
- `backend/src/main/java/com/klabis/events/infrastructure/jdbc/EventRepositoryAdapter.java` — `buildNonFulltextConditions` adds `event_type_id IN (...)` predicate
- `backend/src/main/java/com/klabis/events/infrastructure/restapi/EventController.java` — `listEvents` gains `List<UUID> eventTypeId` param; `buildFilter` applies it; self link updated; two `methodOn` call-sites updated to 13-arg signature
- `backend/src/test/java/com/klabis/events/infrastructure/jdbc/EventJdbcRepositoryTest.java` — new `FilterByEventTypeIds` nested class (4 tests)
- `backend/src/test/java/com/klabis/events/domain/EventFilterTest.java` — one constructor call updated (extra null arg)

---

### B7.4 frontend — "Typ akce" multi-select filter (2026-05-12)

Task B7.4 frontend unblocked and completed. 7 new tests (4 in `EventsFilterBar`, 1 in `EventsPage`); 2 pre-existing `EventsPage` tests updated for query specificity. Full suite 1435/1435.

**Implementation:**
- `EventsFilterBar` gains `eventTypeIds: string[]` on `EventsFilterValue` and optional `eventTypes?: EventTypeCatalogItem[]` prop. When `eventTypes` is non-empty, a `<select multiple>` is rendered with a placeholder option and one option per catalog item. Selection changes produce `onChange` with updated `eventTypeIds`.
- `EventsPage` reads `?eventTypeId=` (multi-value via `searchParams.getAll`) from URL, passes as `eventTypeIds` in `filterValue`. `handleFilterChange` clears and re-appends all selected ids. `extraParams` typed to `Record<string, string | string[]>`; `eventTypeId` set as `string[]` when non-empty.
- `HalEmbeddedTable.extraParams` extended from `Record<string, string>` to `Record<string, string | string[]>`. Array values use `url.searchParams.append()` to produce repeated params; string values use `set()` unchanged.
- Label `eventsFilter.eventTypeFilter = 'Typ akce'` added to `labels.ts`.

**Changed files:**
- `frontend/src/localization/labels.ts` — added `eventsFilter.eventTypeFilter`
- `frontend/src/components/HalNavigator2/HalEmbeddedTable.tsx` — `extraParams` type extended; `append` used for array values
- `frontend/src/components/events/EventsFilterBar.tsx` — `EventsFilterValue.eventTypeIds` added; `eventTypes` prop + multi-select rendered
- `frontend/src/pages/events/EventsPage.tsx` — `eventTypes` destructured from `useEventTypes`; URL read/write for `eventTypeId`; `extraParams` wired
- `frontend/src/components/events/EventsFilterBar.test.tsx` — 4 new tests for "Typ akce" multi-select
- `frontend/src/pages/events/EventsPage.test.tsx` — 1 new test (URL persistence); 2 tests updated (specificity fix for `getByText` now matching `<option>` too)

---

### Iter 8 — Code review fixes (2026-05-12)

All 5 blocking/warning review findings addressed. Backend: 2525/2526 pass (1 pre-existing failure unchanged). Frontend: 1435/1435.

**BLOCKING #1 — Read endpoints authority loosened to EVENTS_READ**
- `listEventTypes()` and `getEventType()` now require `EVENTS_READ` (was `EVENTS_MANAGE`). Regular members can call both endpoints — this fixes the 403s in events list badge, type filter dropdown, and event form.
- `EventTypesRootPostprocessor` still gates the admin nav link at `EVENTS_MANAGE` via an explicit `SecurityContextHolder` authority check before calling `klabisLinkTo`. The nav link "Typy akcí" appears only for admins.
- Tests updated: all GET tests now use `EVENTS_READ` authority; admin tests use `{EVENTS_READ, EVENTS_MANAGE}` (realistic).

**BLOCKING #3 — Case-insensitive DB index (H2 incompatibility — kept at app layer)**
- `CREATE UNIQUE INDEX ON event_types (LOWER(name))` was attempted but H2 2.4.240 rejects functional indexes even in `MODE=PostgreSQL`. Reverted to plain `idx_event_types_name ON event_types (name)`. Application-layer `existsByNameIgnoreCase` remains the enforcing mechanism. Comment added in V001 explaining the trade-off.

**WARNING #4 — Controller refactored to postprocessor pattern**
- `EventTypeDetailsPostprocessor` (new `@MvcComponent`) handles all self/collection links and update/delete affordances for both `listEventTypes` and `getEventType`. Controller now calls `entityModelWithDomain(dto, eventType)`. Inline link-building removed from controller methods.

**WARNING #5 — BulkSyncOrisModal double-fire fixed**
- Added `reset()` call before `mutate()` in the `useEffect`. Stale mutation state is cleared on each open. `syncUrl` prop type widened to `string | undefined`; guard `if (isOpen && syncUrl)` prevents fire with undefined URL. Tests updated to include `reset: vi.fn()` in all mocks.

**WARNING #7 — Hardcoded fallback URL removed**
- `EventsPage.tsx` line 349: `?? '/api/events/sync-from-oris/all-upcoming'` fallback removed. `syncUrl={bulkSyncTemplate.target}`. Template is already gated by `{bulkSyncTemplate && ...}`.

**SUGGESTION #8 — EventTypeInUseException no longer exposes UUIDs**
- Error message changed from `"Event type '<UUID>' is still in use by events: <names>"` to `"Event type is still used by: <names>"`. UUID removed; only event names remain in the 409 response body.

**Changed files:**
- `backend/src/main/resources/db/migration/V001__initial_schema.sql` — plain unique index retained, expanded comment
- `backend/src/main/java/com/klabis/events/eventtype/domain/EventTypeInUseException.java` — UUID removed from message
- `backend/src/main/java/com/klabis/events/eventtype/infrastructure/restapi/EventTypeController.java` — GET endpoints use EVENTS_READ; links moved to `EventTypeDetailsPostprocessor`; `EventTypesRootPostprocessor` gates nav link at EVENTS_MANAGE
- `backend/src/test/java/com/klabis/events/eventtype/infrastructure/restapi/EventTypeControllerTest.java` — tests aligned with EVENTS_READ authority
- `backend/src/test/java/com/klabis/events/eventtype/infrastructure/restapi/EventTypesRootPostprocessorTest.java` — rewritten with 3 authority-aware tests
- `frontend/src/components/events/BulkSyncOrisModal.tsx` — `reset()` before `mutate()`; `syncUrl` prop type `string | undefined`
- `frontend/src/components/events/BulkSyncOrisModal.test.tsx` — `reset: vi.fn()` added to all mocks
- `frontend/src/pages/events/EventsPage.tsx` — fallback URL removed

---
