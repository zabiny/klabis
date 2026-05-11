## Phase A — Bulk ORIS Sync (N5)

### A1. Backend service + endpoint

- [ ] A1.1 Add `OrisBulkSyncService` (application layer in `com.klabis.events.application` or under existing ORIS sync package); method `syncAllUpcoming()` returns `BulkSyncResult(totalProcessed, successCount, failureCount, results)`
- [ ] A1.2 Service iterates events matching `status IN (DRAFT, ACTIVE) AND eventDate >= today AND orisEventId IS NOT NULL`, delegates to existing per-event ORIS sync, catches per-event exceptions, accumulates result list
- [ ] A1.3 Add REST endpoint `POST /api/events/sync-from-oris/all-upcoming` (or chosen path) on `EventController` (or new `OrisEventController`); guarded with `@HasAuthority(EVENTS_MANAGE)`; returns `BulkSyncResult` as JSON
- [ ] A1.4 Add HAL+FORMS affordance `bulk-sync-oris` to events list response, exposed only to callers with EVENTS:MANAGE
- [ ] A1.5 Integration test: 3 matching events, all sync OK → response has successCount=3, failureCount=0
- [ ] A1.6 Integration test: 3 matching events, 1 fails (mock ORIS client to throw) → successCount=2, failureCount=1, results list contains the failed event id + error
- [ ] A1.7 Integration test: events with status FINISHED / CANCELLED / past date / non-ORIS → not processed
- [ ] A1.8 Run tests via test-runner

### A2. Frontend toolbar action + progress modal

- [x] A2.1 In `EventsListPage` toolbar render the affordance from HAL response (button "Synchronizovat všechny budoucí z ORIS")
- [x] A2.2 Click opens a modal with progress spinner and "Synchronizuji..." message; calls the endpoint
- [x] A2.3 On response, modal switches to summary view: "X úspěšně synchronizováno, Y chyb" + list of failures (event name + error)
- [x] A2.4 Frontend tests: button visible only when affordance is present; progress + summary states

### A3. End-to-end verification (Phase A)

- [ ] A3.1 Deploy to `https://api.klabis.otakar.io`
- [ ] A3.2 Browser test: log in as admin, open events list, trigger bulk sync, verify modal flow and summary
- [ ] A3.3 Verify by inspecting a synced event's detail that ORIS data was actually refreshed (e.g. event description changed if updated upstream)

## Phase B — Event Types catalog + filtering (N13)

### B1. DB migration + EventType aggregate

- [x] B1.1 Update `V001__initial_schema.sql` in place: add `event_types` table (id, name, color, sort_order, audit fields, unique index on name) — production runs on H2 without persistent data, in-place migration is OK
- [x] B1.2 Add column `event_type_id UUID NULL REFERENCES event_types(id)` to `events` table
- [x] B1.3 Create domain `EventType` aggregate (`@AggregateRoot`) under `com.klabis.events.eventtype.domain`; record-based with validation (name 1..100 chars, color hex pattern)
- [x] B1.4 Create `EventTypeId` value object
- [x] B1.5 Define `EventTypeRepository` port (find by id, find all sorted, find by name case-insensitive, exists by name, existsEventReferencingType, findEventNamesReferencingType)
- [x] B1.6 Domain unit tests for `EventType` invariants

### B2. Persistence + application service

- [x] B2.1 Create `EventTypeMemento` + `EventTypeRepositoryAdapter` (Spring Data JDBC) — follow backend-patterns Memento approach
- [x] B2.2 Create `EventTypeManagementService` (application layer) implementing CRUD operations; `delete` checks `events.event_type_id` references via repository query, throws `EventTypeInUseException` (with affected event names) if any
- [x] B2.3 Persistence integration test (H2 in-memory, test profile): save/load roundtrip; unique-name violation; case-insensitive query detection; delete round-trip

### B3. REST controller + HAL forms

- [x] B3.1 Create `EventTypeController` under `com.klabis.events.eventtype.infrastructure.restapi`; endpoints: GET list, GET by id, POST create, PUT update, DELETE; gated with `@HasAuthority(EVENTS_MANAGE)`
- [x] B3.2 Define request/response DTOs and HAL+FORMS templates (EventTypeDto, EventTypeIdMixin, EventTypeDtoMapper, EventTypeExceptionHandler)
- [x] B3.3 Controller integration tests for happy paths, unauthorized access, delete-in-use 409

### B4. Event aggregate — type assignment

- [ ] B4.1 Add `Optional<EventTypeId> eventType` to `Event` aggregate; update create/update commands and factories
- [ ] B4.2 Update `EventMemento` to map `event_type_id` column
- [ ] B4.3 Update Event REST DTO + HAL forms templates to include `eventTypeId` (optional in create/update; populated in response with id + name + color resolved from the catalog)
- [ ] B4.4 Integration tests: create event with type, update event to change type, clear type

### B5. ORIS import auto-mapping

- [ ] B5.1 Identify the source field in the ORIS event payload (Level / Discipline) — document the chosen field in design.md
- [ ] B5.2 Implement `EventTypeAutoMapper` helper that calls `EventTypeRepository.findByNameIgnoreCase(orisIdentifier)` during import; sets `event.eventType` to the match or leaves it empty
- [ ] B5.3 No auto-create of catalog entries during import
- [ ] B5.4 Integration test (mocked ORIS client): import with matching catalog entry → event has type; import with no match → event has no type; case-insensitive matching

### B6. Frontend — admin page for event types

- [ ] B6.1 Add navigation link "Typy akcí" under ADMINISTRACE (visible to EVENTS:MANAGE only)
- [ ] B6.2 Create `EventTypesPage` (table list with sort by sort_order, name, color preview); reuse `KlabisTable` and HAL navigator patterns
- [ ] B6.3 Create modal forms for create/edit (name, color picker, sort_order)
- [ ] B6.4 Delete with confirm dialog; show backend 409 conflict message when in use
- [ ] B6.5 Frontend tests for the admin page

### B7. Frontend — events form + table column + filter

- [ ] B7.1 Update event create/update Formik form with type dropdown loaded from `/api/event-types` (cached via TanStack Query)
- [ ] B7.2 Update events table: new column "Typ" with color badge + name; column header conditionally rendered (responsive — hide on small screens?)
- [ ] B7.3 Update event detail page: show type badge in header section
- [ ] B7.4 Update events list filter bar: multi-select "Typ akce" filter; persists in URL same way as other filters
- [ ] B7.5 Update `src/localization/labels.ts` with new labels

### B8. End-to-end verification (Phase B)

- [ ] B8.1 Deploy to `https://api.klabis.otakar.io`
- [ ] B8.2 Browser test (admin): create event types "Trénink", "Pohárový závod", "Mistrovství" with distinct colors
- [ ] B8.3 Browser test (admin): create new manual event with type "Pohárový závod"; verify badge in detail and list
- [ ] B8.4 Browser test (admin): import an ORIS event whose Level/Discipline matches one of the catalog entries; verify auto-mapping
- [ ] B8.5 Browser test (admin): try to delete a type that is in use → expect 409 with informative message
- [ ] B8.6 Browser test (member): filter list by "Pohárový závod"; verify only matching events show

## 9. Documentation

- [ ] 9.1 Update `docs/developerManual` with the chosen ORIS field for type auto-mapping and the EventType catalog overview
- [ ] 9.2 Sync the spec changes into `openspec/specs/events/spec.md` and create `openspec/specs/event-types/spec.md` after archiving
