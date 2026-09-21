# Tasks

## 1. Local Discipline catalog

- [x] 1.1 Write a failing unit test for the new `Discipline` domain aggregate (create with code/name, no `orisId` field, defaults to not archived) in `events.domain`, then implement `Discipline` and `DisciplineId` to make it pass
- [x] 1.2 Add `events.disciplines(id UUID PK, code, name, archived, audit columns)` to `V001__initial_schema.sql`; add `DisciplineMemento`, `DisciplineJdbcRepository`, `DisciplineRepositoryAdapter`; verify a new `DisciplineRepositoryAdapterTest` (save + `findById`) passes against H2

## 2. EventType references local disciplines instead of raw ORIS ints

- [x] 2.1 Change `EventType.orisDisciplineIds: Set<Integer>` to `disciplineIds: Set<DisciplineId>` (and the `CreateEventType`/`UpdateEventType` commands); update `EventTypeTest` to use `DisciplineId` fixtures and verify it passes
- [x] 2.2 Change `event_type_oris_disciplines.discipline_id` in `V001__initial_schema.sql` to a `UUID` FK into `events.disciplines`; update `OrisDisciplineMemento`, `EventTypeMemento`, `EventTypeJdbcRepository`, and rename `EventTypeRepositoryAdapter.findByOrisDisciplineId(int)` to `findByDisciplineId(DisciplineId)`; verify `EventTypeRepositoryAdapterTest` passes with `DisciplineId` fixtures instead of raw ints
- [x] 2.3 Update `EventTypeManagementService.validateNoDisciplineIdConflict` and `OrisDisciplineAlreadyMappedException` to work in terms of `DisciplineId`; verify `EventTypeManagementServiceTest`'s conflict-detection scenarios pass with `DisciplineId` fixtures
- [x] 2.4 Update the `EventTypeController` REST contract for the field renamed by D2: in `docs/openapi/spec/events.yaml`, change `EventTypeDto`/`CreateEventTypeRequest`/`UpdateEventTypeRequest`'s `orisDisciplineIds: integer[]` to `disciplineIds: uuid[]` (and the `x-hal-templates` description text referencing "ORIS discipline options"); regenerate the API interfaces; update `CreateEventTypeRequestConverter`/`UpdateEventTypeRequestConverter`/`EventTypeDtoConverter` to map the field directly (no sync-engine lookup involved, per design.md D2) instead of the `ignore = true` placeholders task 2.1-2.3 may have left; update `EventTypeControllerTest` to assert real pass-through of `disciplineIds` instead of the interim empty/ignored behaviour
- [x] 2.5 Follow through on 2.4's frontend break: regenerate `frontend`'s `klabisApi.d.ts`/`halTypes.ts` from the updated `events.yaml` (`npm run openapi` or repo equivalent — check `frontend/CLAUDE.md`); update the two hardcoded `"orisDisciplineIds"` string references (the event-type form's `HalFormsCheckboxGroup` property lookup and the `labels.ts` display label) to `"disciplineIds"`, and their tests (`HalFormsCheckboxGroup.test`, `HalFormsFieldFactory.test.tsx`) — no UX/behaviour change, purely following the renamed/retyped property; rebuild the bundled frontend assets under `backend/src/main/resources/static` (`publish-frontend-resources`, per root CLAUDE.md) so the backend serves the updated bundle; verify via the frontend test suite

## 3. Discipline picklist reads local data

- [x] 3.1 Add `DisciplineRepository.findAllSorted()` (non-archived only) to `events.domain`; rewrite `EventTypeManagementService.listDisciplineOptions()` to read it instead of calling `orisApiClient.get().listDisciplines()`, removing the `Optional<OrisApiClient>` constructor dependency; verify `EventTypeManagementServiceTest`'s discipline-options tests pass against local fixtures with no ORIS mock involved, and that an archived discipline is excluded

## 4. Discipline synchronisation adapter

- [x] 4.1 Add `SyncEntityType.DISCIPLINE("disciplines")` next to `EVENT`; verify existing `SyncEntityType`-based tests still pass unchanged
- [x] 4.2 Add `DisciplineProjection` (code, name) and a mapper from ORIS's `DisciplineListEntry`/`Discipline` DTOs and from the local `Discipline` aggregate; write unit tests for both mapping directions
- [x] 4.3 Implement `DisciplineSyncAdapter` (`events.infrastructure.orissync`) — `SyncCapabilities.pullOnlyCreating()`, `readLocal`, `readExternal`, `applyToLocal`, `createLocal`, `applyToExternal` throwing `UnsupportedOperationException` — mirroring `OrisEventSyncAdapterTest`'s structure; verify a new `DisciplineSyncAdapterTest` covers create, update and the no-outward-write case

## 5. Automatic discovery of new ORIS disciplines

- [x] 5.1 Implement `DisciplineDiscoveryJob`: call `orisApiClient.listDisciplines()`, filter out ids already returned by `SynchronizationPort.findByExternalReferences(DISCIPLINE, ORIS, ids)`, call `pullAndEnroll(DISCIPLINE, externalReference, null)` for each remaining id; write a unit test verifying only undiscovered ids trigger `pullAndEnroll`
- [x] 5.2 Schedule `DisciplineDiscoveryJob` on its own `klabis.disciplines.discovery-cron` property (default nightly, independent of `klabis.sync.scan-cron`), externalized via `KLABIS_DISCIPLINES_DISCOVERY_CRON`; verify it runs on schedule via a Spring context test
- [x] 5.3 Write an integration test (mirroring `OrisEventSyncScenarioIntegrationTest`) proving specs/disciplines' "new ORIS discipline appears automatically" and "name change follows ORIS" scenarios end-to-end: discovery creates the local `Discipline`, a later pass updates its name

## 6. Resolve an event's ORIS discipline through the sync pairing

- [x] 6.1 Update `OrisEventFieldsReader.resolveEventTypeFromOrisDiscipline` to resolve the ORIS discipline id via `SynchronizationPort.findByExternalReferences(DISCIPLINE, ORIS, ...)` then `EventTypeRepository.findByDisciplineId(DisciplineId)`, returning `null` when the discipline is not yet paired (same as today's "no match" branch); update `OrisEventFieldsReaderTest` to mock `SynchronizationPort` instead of `EventTypeRepository.findByOrisDisciplineId`

## 7. Archiving a discipline never breaks an EventType's reference

- [x] 7.1 Add `archived: boolean` to `Discipline`, plus `archive()`/`restore()` domain methods and a `DisciplineArchivedEvent`; write a failing unit test asserting `archive()` flips the flag and publishes the event, then implement to pass
- [x] 7.2 Update `DisciplineMemento`/persistence for the new column; write an integration test proving an `EventType` still referencing an archived `Discipline`'s id loads and saves correctly (the FK is never touched by archiving)

## 8. Archiving and restoring reuse the sync engine's retire/reactivate lifecycle

- [x] 8.1 Implement `DisciplineSyncListener` (mirrors `EventsSyncListener`): on `DisciplineArchivedEvent`, call `SynchronizationPort.retire` for the discipline's `SyncRecord` if one exists (no-op otherwise); write a unit test covering both the paired and unpaired cases
- [x] 8.2 Implement the restore path: look up the discipline's `SyncRecord` via `SynchronizationPort.findByTarget` and, if found, call `pullAndEnroll` again to reactivate it (unpaired disciplines just clear the `archived` flag); write a unit test covering both cases
- [x] 8.3 Write an integration test proving specs/disciplines' "manager removes a discipline that is still assigned" and "manager restores a removed discipline" scenarios end-to-end, including that the reactivated pairing's next sync pass runs correctly

## 9. Discipline CRUD REST API

- [x] 9.1 Add `DisciplineNotEditableException` (`events.domain`); add a paginated query to `DisciplineRepository` alongside `findAllSorted()` (D10); update `DisciplineManagementPort`/`DisciplineManagementService` (`events.application`) with `create`, `update`, `archive`, `restore`, `list(Pageable): Page<Discipline>`, `get`, mirroring `EventTypeManagementPort`/`Service`; `update` calls `SynchronizationPort.findByTarget` and throws `DisciplineNotEditableException` when the discipline is ORIS-paired; write unit tests for each operation including "archive always succeeds even if referenced", "restore fails with 409-equivalent if not archived", "update succeeds for a manually created discipline", "update is refused for an ORIS-paired discipline", and "list returns the requested page"
- [x] 9.2 Add the `Discipline` resource to `docs/openapi/spec/events.yaml` (`GET`/`POST /api/disciplines`, `GET`/`PUT`/`DELETE /api/disciplines/{id}`, `POST /api/disciplines/{id}/restore`) with `x-klabis-authority` (`EVENTS_READ`/`EVENTS_MANAGE`), `x-hal-links` (including `sync`) and `x-hal-templates` per design.md's API Changes table; `GET /api/disciplines` uses `x-spring-paginated: true` with `PageParam`/`SizeParam` (D10), same shape as `listEvents`; add a `DisciplineExceptionHandler` mapping `DisciplineNotEditableException` to `409 Conflict`, mirroring `EventTypeExceptionHandler`; regenerate the API interfaces and verify the build compiles
- [x] 9.3 Implement `DisciplineController` implementing the generated `DisciplinesApi`, with HAL links (`self`, `collection`, and standard paging links `first`/`last`/`next`/`prev` on the list response) and conditional affordances (`createDiscipline`, `updateDiscipline` only when not ORIS-paired, exactly one of `archiveDiscipline`/`restoreDiscipline`) gated by authority and archived/paired state; write a `@WebMvcTest` verifying the HAL+FORMS shape for an active manually-created discipline, an archived discipline, an ORIS-paired discipline (no `updateDiscipline` template), and that `listDisciplines` returns a correctly paged response with paging links
- [ ] 9.4 Add `EnrolledDisciplineIds` (`events.infrastructure.restapi`), mirroring `EnrolledEventIds`: `getDiscipline` populates it via one `SynchronizationPort.findByTarget` call, `listDisciplines` via one batched `findActiveByTargets` call for the page; the shared postprocessor adds a `sync` link (`SyncApi.getSyncState(SyncEntityTypeParam.DISCIPLINES, id)`) to every `DisciplineDto` whose id is in that set, in both the collection and detail responses; write a `@WebMvcTest` verifying the `sync` link appears for a paired discipline and is absent for a manually created one, in both `listDisciplines` and `getDiscipline`

## 10. Cleanup and full verification

- [ ] 10.1 Grep the backend for any remaining direct calls to `orisApiClient...listDisciplines()` outside `DisciplineSyncAdapter`/`DisciplineDiscoveryJob` and remove them; verify none remain
- [ ] 10.2 Run the full backend test suite via the `test-runner` agent and fix any regressions until it is green
- [ ] 10.3 Start the app locally (clean DB, `oris` profile active) and confirm via the event type edit form that the discipline picklist shows ORIS disciplines discovered automatically, with no live ORIS call on page load (verify by checking backend logs / temporarily blocking ORIS reachability); also exercise the new `/api/disciplines` CRUD endpoints manually (create, archive, restore) and confirm an archived discipline still referenced by an event type keeps working, an ORIS-discovered discipline has a `sync` link and cannot be edited (`PUT` returns 409), and a manually created discipline has no `sync` link and can be edited
