# Tasks

## 1. Local Discipline catalog

- [ ] 1.1 Write a failing unit test for the new `Discipline` domain aggregate (create with code/name, no `orisId` field) in `events.domain`, then implement `Discipline` and `DisciplineId` to make it pass
- [ ] 1.2 Add `events.disciplines(id UUID PK, code, name, audit columns)` to `V001__initial_schema.sql`; add `DisciplineMemento`, `DisciplineJdbcRepository`, `DisciplineRepositoryAdapter`; verify a new `DisciplineRepositoryAdapterTest` (save + `findById`) passes against H2

## 2. EventType references local disciplines instead of raw ORIS ints

- [ ] 2.1 Change `EventType.orisDisciplineIds: Set<Integer>` to `disciplineIds: Set<DisciplineId>` (and the `CreateEventType`/`UpdateEventType` commands); update `EventTypeTest` to use `DisciplineId` fixtures and verify it passes
- [ ] 2.2 Change `event_type_oris_disciplines.discipline_id` in `V001__initial_schema.sql` to a `UUID` FK into `events.disciplines`; update `OrisDisciplineMemento`, `EventTypeMemento`, `EventTypeJdbcRepository`, and rename `EventTypeRepositoryAdapter.findByOrisDisciplineId(int)` to `findByDisciplineId(DisciplineId)`; verify `EventTypeRepositoryAdapterTest` passes with `DisciplineId` fixtures instead of raw ints
- [ ] 2.3 Update `EventTypeManagementService.validateNoDisciplineIdConflict` and `OrisDisciplineAlreadyMappedException` to work in terms of `DisciplineId`; verify `EventTypeManagementServiceTest`'s conflict-detection scenarios pass with `DisciplineId` fixtures

## 3. Discipline picklist reads local data

- [ ] 3.1 Add `DisciplineRepository.findAllSorted()` (or equivalent) to `events.domain`; rewrite `EventTypeManagementService.listDisciplineOptions()` to read it instead of calling `orisApiClient.get().listDisciplines()`, removing the `Optional<OrisApiClient>` constructor dependency; verify `EventTypeManagementServiceTest`'s discipline-options tests pass against local fixtures with no ORIS mock involved

## 4. Discipline synchronisation adapter

- [ ] 4.1 Add `SyncEntityType.DISCIPLINE("disciplines")` next to `EVENT`; verify existing `SyncEntityType`-based tests still pass unchanged
- [ ] 4.2 Add `DisciplineProjection` (code, name) and a mapper from ORIS's `DisciplineListEntry`/`Discipline` DTOs and from the local `Discipline` aggregate; write unit tests for both mapping directions
- [ ] 4.3 Implement `DisciplineSyncAdapter` (`events.infrastructure.orissync`) — `SyncCapabilities.pullOnlyCreating()`, `readLocal`, `readExternal`, `applyToLocal`, `createLocal`, `applyToExternal` throwing `UnsupportedOperationException` — mirroring `OrisEventSyncAdapterTest`'s structure; verify a new `DisciplineSyncAdapterTest` covers create, update and the no-outward-write case

## 5. Automatic discovery of new ORIS disciplines

- [ ] 5.1 Implement `DisciplineDiscoveryJob`: call `orisApiClient.listDisciplines()`, filter out ids already returned by `SynchronizationPort.findByExternalReferences(DISCIPLINE, ORIS, ids)`, call `pullAndEnroll(DISCIPLINE, externalReference, null)` for each remaining id; write a unit test verifying only undiscovered ids trigger `pullAndEnroll`
- [ ] 5.2 Schedule `DisciplineDiscoveryJob` (reuse `klabis.sync` scheduling config or its own cron property) and verify it runs on application startup/schedule via a Spring context test
- [ ] 5.3 Write an integration test (mirroring `OrisEventSyncScenarioIntegrationTest`) proving specs/disciplines' "new ORIS discipline appears automatically" and "name change follows ORIS" scenarios end-to-end: discovery creates the local `Discipline`, a later pass updates its name

## 6. Resolve an event's ORIS discipline through the sync pairing

- [ ] 6.1 Update `OrisEventFieldsReader.resolveEventTypeFromOrisDiscipline` to resolve the ORIS discipline id via `SynchronizationPort.findByExternalReferences(DISCIPLINE, ORIS, ...)` then `EventTypeRepository.findByDisciplineId(DisciplineId)`, returning `null` when the discipline is not yet paired (same as today's "no match" branch); update `OrisEventFieldsReaderTest` to mock `SynchronizationPort` instead of `EventTypeRepository.findByOrisDisciplineId`

## 7. Cleanup and full verification

- [ ] 7.1 Grep the backend for any remaining direct calls to `orisApiClient...listDisciplines()` outside `DisciplineSyncAdapter`/`DisciplineDiscoveryJob` and remove them; verify none remain
- [ ] 7.2 Run the full backend test suite via the `test-runner` agent and fix any regressions until it is green
- [ ] 7.3 Start the app locally (clean DB, `oris` profile active) and confirm via the event type edit form that the discipline picklist shows ORIS disciplines discovered automatically, with no live ORIS call on page load (verify by checking backend logs / temporarily blocking ORIS reachability)
