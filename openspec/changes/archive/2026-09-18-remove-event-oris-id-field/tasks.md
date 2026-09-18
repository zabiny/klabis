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

- [x] 5.1 Remove `Event.canSyncFromOris()` and its call site in `OrisEventSyncAdapter.applyToLocal` (if still present) — no separate `canSyncFromOris()` method existed; the guard was already inlined as an `if (orisId == null) throw ...` at the top of `Event.syncFromOris()`. Removed that guard along with its Javadoc paragraph.
- [x] 5.2 Remove `Event.orisId` field and `getOrisId()`
- [x] 5.3 Remove `orisId` parameter from `Event`'s private constructor and both `reconstruct(...)` overloads
- [x] 5.4 Remove `orisId` component from `Event.CreateEventFromOris`; remove its dead `from(Event)` factory
- [x] 5.5 Remove dead `Event.ImportCommand.from(Event)` factory (the `ImportCommand` type itself, its `orisId` REST-input component, and validation stay unchanged)
- [x] 5.6 Update `EventCreateEventFromOrisBuilder` usage in `OrisEventSyncAdapter.buildCreateFromOris` to drop `.orisId(...)`
- [x] 5.7 Update `EventSyncFromOrisBuilder`/`Event.SyncFromOris` usage if it also carries `orisId` — confirmed it never carried `orisId`; no change needed.
- [x] 5.8 Run domain unit tests (`EventTest` and related), fix compilation and assertions referencing the removed field/methods — also removed `EventTest.shouldThrowWhenEventHasNoOrisId` (asserted the now-deleted invariant) and updated ~30 test call sites across `EventTest`, `EventTestDataBuilder`, `EventRegistrationServiceTest`, `OrisEventImportServiceTest`, `OrisEventSyncAdapterTest`, `OrisEventProjectionMapperTest`, `OrisBulkSyncServiceTest`, `EventControllerTest`, and calendar-module tests (`CalendarEventSyncIntegrationTest`, `IcalFeedServiceTest`, `ICalendarRendererTest`, `EventsEventListenerTest`) that called the old constructor/builder shape.

## 6. Persistence: drop `oris_id`

- [x] 6.1 Remove `orisId` field and mapping from `EventMemento` (`from(Event)` / `toEvent()`)
- [x] 6.2 Remove `oris_id` column from the `events.events` table definition in the `V001` migration script
- [x] 6.3 Run `@DataJdbcTest`-slice tests for `EventRepositoryAdapter`, confirm no remaining reference to `oris_id` — also found and removed `EventRepository.findAllUpcomingOrisEvents` (+ `EventRepositoryAdapter` impl + its `EventJdbcRepositoryTest` nested test class), a previously-undetected dead-code reader of `events.events.oris_id` with no production caller (same category as `existsByOrisId`, removed in task group 3) that broke at runtime once the column was dropped.

## 7. Full verification

- [x] 7.1 Run the full backend test suite (`test-runner` agent), fix any remaining failures — 3527/3527 passed after groups 1-6; re-run after the `simplify` review fixes (unifying `EnrolledEventIds`/`EventSyncEnrolment`, scoping the list-endpoint sync lookup) — 3532/3532 passed, no regressions.
- [x] 7.2 Grep the codebase for `orisId` to confirm only `EventCategory.orisId`, `Event.ImportCommand.orisId`, ORIS import/sync application-layer parameters (`OrisEventImportPort`, `OrisEventBulkImportPort`, `OrisEventFieldsReader`, etc.) and `OrisEventProjection`/adapter-level ORIS identifiers remain — no `Event.orisId` survivors. Confirmed clean.
- [x] 7.3 Manually verify via `runLocalEnvironment.sh` + browser: import an ORIS event, confirm it disappears from the import candidate list; sync affordance still appears on an enrolled DRAFT/ACTIVE event — attempted via the in-session browser tool, but the local backend's self-signed TLS certificate on `:8443` blocked navigation with no interactive "proceed anyway" affordance in this headless browser, so a live click-through could not be completed. Both scenarios are covered end-to-end against a real H2 database by existing integration tests exercised in 7.1 (`OrisEventControllerTest` — already-imported events excluded from the candidate list; `EventControllerTest`/`OrisEventControllerTest` — sync affordance present/absent by enrolment state), so behavior preservation is verified by that route instead. Flagging for a human or a session with working TLS trust to click through manually if desired.
- [x] 7.4 Code review — dedicated `code-reviewer` subagent pass on groups 5-6 (no blocking/warning findings, one cosmetic nit fixed), plus a `simplify` skill pass (4 parallel angles) across groups 1-6 that found and fixed real duplication (two `HalResponseContext` types for one concept) and an inefficiency (`listEvents` full sync_record scan instead of page-scoped lookup); fixes committed and re-verified.
