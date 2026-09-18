## 1. Adapter can create the local side

- [x] 1.1 RED: adapter test asserting the default `createLocal` refuses, so an integration that has not opted in cannot silently create entities
- [x] 1.2 GREEN: add `createLocal(SyncProjection)` returning the new entity identifier to `SynchronizationAdapter`, defaulting to a refusal
- [x] 1.3 RED: test asserting `SyncCapabilities.pullOnlyCreating()` declares reads both sides, writes local only, creates local, creates nothing external
- [x] 1.4 GREEN: add the `pullOnlyCreating()` factory
- [x] 1.5 REFACTOR: check the new factory reads consistently alongside `pullOnly()` and `bidirectional()`

## 2. A retired pairing can be brought back

- [x] 2.1 RED: domain test — `reactivate()` on a retired pairing clears retirement, discards the baseline, and returns a `ScheduleEffect` that makes the pairing due again
- [x] 2.2 GREEN: add `SyncRecord.reactivate()` (design D6)
- [x] 2.3 RED: domain test — `reactivate()` is refused on a pairing that is not retired
- [x] 2.4 GREEN: add the state guard
- [x] 2.5 RED: test — the pass following a reactivation adopts the external side, exactly as for a fresh pairing. This is what proves the baseline was discarded rather than merely ignored; make sure it fails before 2.2 is in place
- [x] 2.6 GREEN: whatever 2.5 exposes
- [x] 2.7 REFACTOR: keep `retire`/`reactivate` symmetric and adjacent in the aggregate

## 3. A pairing can be found from the external side

- [x] 3.1 RED: repository test — `findBySystemAndExternalId` finds active, conflicted, failed and retired pairings, and returns empty when nothing is paired. Retired must be included; assert it explicitly, since excluding it is the easy mistake and everything in section 5 depends on it
- [x] 3.2 GREEN: add the method to `SyncRecordRepository` and implement it in `SyncRecordRepositoryAdapter` — no migration, the existing `uq_sync_record_external` constraint backs the query
- [x] 3.3 REFACTOR: confirm the query plan uses that constraint's index rather than scanning

## 4. Bringing in a record Klabis does not have

- [x] 4.1 RED: integration test — the operation creates the entity, links the pairing, establishes the baseline, and records exactly one attempt carrying the acting user
- [x] 4.2 GREEN: add `pullAndEnroll(SyncEntityType, ExternalReference, String actingUser)` to `SynchronizationPort` and implement the no-pairing branch: resolve the adapter, `readExternal`, `createLocal`, `enroll`, then run the ordinary pass (design D3, D4, D8)
- [x] 4.3 RED: integration test — an adapter that does not declare `createsLocal` has the operation refused, and nothing is created
- [x] 4.4 GREEN: add the capability gate
- [x] 4.5 RED: integration test — no adapter registered for the entity type and system is refused with a clear exception
- [x] 4.6 GREEN: add the registry lookup failure
- [x] 4.7 RED: integration test — the external system cannot be read: no entity is created and no pairing is left behind
- [x] 4.8 GREEN: whatever 4.7 exposes
- [x] 4.9 REFACTOR: keep the external calls outside every transaction boundary; creation and enrolment commit together, the pass commits its own outcome

## 5. Bringing in a record that is already linked

- [ ] 5.1 RED: integration test — repeating the operation on an in-step pairing creates no second entity, runs an ordinary pass, and returns the existing pairing
- [ ] 5.2 GREEN: implement the existing-pairing branch as an ordinary `synchronizeNow`, letting the decision table resolve direction (design D5)
- [ ] 5.3 RED: integration test — a local change to an externally-owned field is not overwritten by a repeat; the pairing ends in conflict. This is the behaviour the whole design turns on, so make sure the test fails loudly if direction is ever forced inward
- [ ] 5.4 GREEN: whatever 5.3 exposes
- [ ] 5.5 RED: integration test — a repeat on a `CONFLICT` pairing and on a `FAILED` pairing is refused, nothing is written, and no attempt is recorded
- [ ] 5.6 GREEN: reuse the existing needs-resolution guard
- [ ] 5.7 RED: integration test — a repeat on a retired pairing returns it to service, adopts the external values, and keeps its earlier history
- [ ] 5.8 GREEN: implement the retired branch using `reactivate()` from section 2, then fall through to the pass
- [ ] 5.9 REFACTOR: the three branches should read as three branches, not as nested conditionals

## 6. A rejected creation stops the pairing

- [ ] 6.1 RED: test — a creation refused by a uniqueness constraint is classified terminal, not retryable, so it is not repeated five times against the same rejection
- [ ] 6.2 GREEN: classify it in `FailureClassifier` (design D9)
- [ ] 6.3 REFACTOR: confirm the classification does not catch unrelated data-integrity failures that genuinely are retryable

## 7. A failed initial pass leaves a completable state

- [ ] 7.1 RED: integration test — when the initial pass fails after the entity was created, the entity and pairing exist without a baseline, and a subsequent scheduled pass completes them (design D8's accepted boundary, stated as a test so it stays true)
- [ ] 7.2 GREEN: whatever 7.1 exposes

## 8. ORIS events create their own side

- [ ] 8.1 RED: adapter test — `createLocal` builds the event from an ORIS projection, applies the auto-mapped event type, and returns its identifier
- [ ] 8.2 GREEN: move the event-creation logic out of `OrisEventImportService` into `OrisEventSyncAdapter.createLocal`
- [ ] 8.3 RED: test — the resolved event type reaches the created event but never the hash or the stored projection
- [ ] 8.4 GREEN: whatever 8.3 exposes
- [ ] 8.5 GREEN: declare `pullOnlyCreating()` in `OrisEventSyncAdapter.capabilities()`
- [ ] 8.6 REFACTOR: verify `OrisEventTypeAutoMappingTest` still passes with creation moved

## 9. ORIS import delegates to the engine

- [ ] 9.1 RED: rewrite the duplicate-import tests in `OrisEventImportServiceTest` and `OrisEventControllerTest` to the new contract — a repeat synchronises and returns the existing event instead of failing. Rewrite, do not delete: the new contract needs the coverage the old one had
- [ ] 9.2 GREEN: rewrite `importEventFromOris` to call `pullAndEnroll`; remove the hand-rolled enrolment and duplicate detection
- [ ] 9.3 RED: controller test — importing an event awaiting a decision is refused with the existing problem detail pointing at the synchronisation resource
- [ ] 9.4 GREEN: map the needs-resolution case onto `EventSyncNeedsResolutionException`
- [ ] 9.5 RED: controller test — a repeat import still answers `201 Created` with a `Location` header, having created nothing (design D10)
- [ ] 9.6 RED: `OrisEventBulkImportServiceTest` — an already-present event counts as imported, not failed, and is not distinguished from a newly created one (design D10)
- [ ] 9.7 GREEN: whatever 9.5 and 9.6 expose
- [ ] 9.8 REFACTOR: decide the fate of `DuplicateOrisImportException` and its `409` handler — after 9.2 the only thing that can raise it is the terminal constraint violation of 6.2, which is a server fault rather than a caller error (design, Open Questions)

## 10. End-to-end behaviour

- [ ] 10.1 RED: extend `OrisEventSyncScenarioIntegrationTest` — an imported event is in step immediately, without waiting for a scheduled run
- [ ] 10.2 RED: same test class — re-importing a cancelled event resumes synchronisation and takes the current ORIS values
- [ ] 10.3 GREEN: whatever 10.1 and 10.2 expose
- [ ] 10.4 Walk every scenario in the `data-synchronization` and `events` delta specs and confirm a test covers it
- [ ] 10.5 Run the full backend suite; confirm no regression beyond the pre-existing known failures
- [ ] 10.6 Code review before commit
