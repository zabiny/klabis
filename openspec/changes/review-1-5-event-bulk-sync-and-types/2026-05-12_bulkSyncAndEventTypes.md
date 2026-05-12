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
| 7 | B7 — Frontend event form/table column/filter/detail badge + labels | frontend-developer | TODO |
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
