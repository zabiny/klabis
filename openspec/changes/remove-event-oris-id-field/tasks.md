## 1. Sync engine: new batch lookup by external reference

- [x] 1.1 Add `SyncedEntityReference(SyncTarget target, ExternalReference externalReference)` value type to `sync.domain`
- [x] 1.2 Write failing test for `SyncRecordRepository.findByExternalReferences(SyncEntityType, ExternalSystem, Collection<String>)` — covers: no match, single match, multiple matches, and a `RETIRED` record still matching
- [x] 1.3 Add `findByExternalReferences` to `SyncRecordRepository` (secondary port)
- [x] 1.4 Implement `SyncRecordJdbcRepository` query (`SELECT external_id, entity_id FROM sync.sync_record WHERE entity_type = :entityType AND external_system = :system AND external_id IN (:externalIds)`, no `retired_at` filter) and wire `SyncRecordRepositoryAdapter` to assemble `SyncedEntityReference` results
- [x] 1.5 Add `findByExternalReferences` to `SynchronizationPort` (primary port), delegating straight through
- [x] 1.6 Run the failing tests from 1.2, confirm green

## 2. Events module: re-point the "already imported" filter

- [x] 2.1 Write/update test for `ImportedOrisEventsService.findImportedOrisIds` asserting it now sources from `SynchronizationPort.findByExternalReferences` instead of `EventRepository`, including the `RETIRED`-still-excluded case
- [x] 2.2 Update `ImportedOrisEventsService` to call `synchronizationPort.findByExternalReferences(EVENT, ORIS, candidateOrisIds as strings)` and map `externalReference().externalId()` back to `Set<Integer>`
- [x] 2.3 Remove `EventRepository.findImportedOrisIds`, `EventJdbcRepository.findImportedOrisIds`, `EventRepositoryAdapter.findImportedOrisIds`
- [x] 2.4 Run `OrisController` / `ImportOrisEventModal`-covering integration tests, confirm the "already imported events are not offered" scenario still passes unchanged

## 3. Events module: remove the dead duplicate-import guard

- [x] 3.1 Confirm (grep) `EventRepository.existsByOrisId` has no remaining callers
- [x] 3.2 Remove `EventRepository.existsByOrisId`, `EventJdbcRepository.existsByOrisId`, `EventRepositoryAdapter.existsByOrisId`

## 4. REST layer: affordance gate reuses existing enrolment flag

- [x] 4.1 Update `EventController.addManagementAffordances` signature to accept `boolean orisEnrolled` instead of reading `event.getOrisId()`
- [x] 4.2 Update both call sites (DRAFT and ACTIVE branches) to use `orisEnrolled` in place of `event.getOrisId() != null`
- [x] 4.3 Update `EventController.getEvent` (and any other caller of `addManagementAffordances`) to pass the `isEnrolled` value already computed via `synchronizationPort.findByTarget(...)` for `EventSyncEnrolment`. `listEvents` (list-row postprocessor `EventSummaryPostprocessor` also calls `addManagementAffordances`) had no equivalent per-row enrolment flag, so this task additionally added one: `synchronizationPort.findActiveByEntityType(EVENT)` is called once in `listEvents`, published as a new `EnrolledEventIds` context record, and read per row.
- [x] 4.4 Run/update `EventController` HAL affordance tests (sync affordance present/absent for enrolled/non-enrolled events), confirm unchanged observable behavior

## 5. Domain: remove `Event.orisId`

- [ ] 5.1 Remove `Event.canSyncFromOris()` and its call site in `OrisEventSyncAdapter.applyToLocal` (if still present)
- [ ] 5.2 Remove `Event.orisId` field and `getOrisId()`
- [ ] 5.3 Remove `orisId` parameter from `Event`'s private constructor and both `reconstruct(...)` overloads
- [ ] 5.4 Remove `orisId` component from `Event.CreateEventFromOris`; remove its dead `from(Event)` factory
- [ ] 5.5 Remove dead `Event.ImportCommand.from(Event)` factory (the `ImportCommand` type itself, its `orisId` REST-input component, and validation stay unchanged)
- [ ] 5.6 Update `EventCreateEventFromOrisBuilder` usage in `OrisEventSyncAdapter.buildCreateFromOris` to drop `.orisId(...)`
- [ ] 5.7 Update `EventSyncFromOrisBuilder`/`Event.SyncFromOris` usage if it also carries `orisId` — confirm and drop if present
- [ ] 5.8 Run domain unit tests (`EventTest` and related), fix compilation and assertions referencing the removed field/methods

## 6. Persistence: drop `oris_id`

- [ ] 6.1 Remove `orisId` field and mapping from `EventMemento` (`from(Event)` / `toEvent()`)
- [ ] 6.2 Remove `oris_id` column from the `events.events` table definition in the `V001` migration script
- [ ] 6.3 Run `@DataJdbcTest`-slice tests for `EventRepositoryAdapter`, confirm no remaining reference to `oris_id`

## 7. Full verification

- [ ] 7.1 Run the full backend test suite (`test-runner` agent), fix any remaining failures
- [ ] 7.2 Grep the codebase for `orisId` to confirm only `EventCategory.orisId`, `Event.ImportCommand.orisId`, ORIS import/sync application-layer parameters (`OrisEventImportPort`, `OrisEventBulkImportPort`, `OrisEventFieldsReader`, etc.) and `OrisEventProjection`/adapter-level ORIS identifiers remain — no `Event.orisId` survivors
- [ ] 7.3 Manually verify via `runLocalEnvironment.sh` + browser: import an ORIS event, confirm it disappears from the import candidate list; sync affordance still appears on an enrolled DRAFT/ACTIVE event
- [ ] 7.4 Code review (code-review skill) before committing
