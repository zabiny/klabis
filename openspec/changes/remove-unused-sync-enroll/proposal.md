## Why

This change started life as `add-enrollment-audit-entry`: enrolling an entity into synchronisation left no trace in the record's history, so an ORIS-imported event showed an empty history until its first scheduled pass, and the proposal wanted a new `ENROLLED` outcome to record the moment of enrolment.

That premise no longer holds. Since `pull-and-enroll-sync-operation` the ORIS event import calls `SynchronizationPort.pullAndEnroll`, which reads ORIS, creates the entity, pairs it and **runs a first synchronisation pass** — an attempt in the history, recorded as `MANUAL`. The history of an imported event therefore starts at the import, and the requirement `Imported ORIS Events Are Synchronized From The Moment They Are Imported` (import appears in the synchronisation history) is met without any new outcome. Every other production caller (disciplines, members, ORIS entries of club members) also goes through `pullAndEnroll`.

What is left is the bare `SynchronizationPort.enroll(SyncTarget, ExternalReference)` operation. It has **no production caller**; its only users are tests that need a paired `SyncRecord` as a fixture:

- `SynchronizationServiceIntegrationTest`
- `SynchronizationServiceNoAdapterIntegrationTest`
- `OrisEventRegistrationSyncScenarioIntegrationTest`

Keeping a public port operation that nothing in production uses invites exactly the misunderstanding that produced the original proposal: that enrolment is a user-visible step with a half-empty history. Removing it makes the port say what the system really does.

## What Changes

- Remove `enroll` from `SynchronizationPort` and its implementation in `SynchronizationService`. Keep the internal creation path `SyncRecordCreator.createAndPair`, which `pullAndEnroll` uses.
- Replace the test usages with a small test fixture that creates the pairing directly (repository plus `SyncRecordCreator`, or a thin test helper), so those tests keep their intent without the removed operation.
- Fix the Javadoc that refers to `enroll` (`SynchronizationPort` class comment and the `state` method comment).
- Drop the `ENROLLED` outcome idea; nothing else changes in `SyncOutcome` or `RetryScheduler`.

No behaviour change: no endpoint, event, history entry or retry behaviour differs.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/data-synchronization/spec.md` — describes bringing a record in (`An External Record Can Be Brought In And Kept In Step`, `Bringing In A Record That Is Already Linked Synchronises It`) and recording every attempt (`Every Synchronisation Attempt Is Recorded`). All of it is delivered by `pullAndEnroll`; no requirement names a bare enrolment without a pass.
- `openspec/specs/events/spec.md` — `Imported ORIS Events Are Synchronized From The Moment They Are Imported` is satisfied by `pullAndEnroll` and is not touched.

**Why no spec update is needed:** the removed operation is not reachable from any endpoint, job or listener; only test code calls it.

## Impact

- `backend/src/main/java/com/klabis/sync/application/SynchronizationPort.java`, `SynchronizationService.java`.
- Three integration tests listed above (fixture setup).

## Related finding, not part of this change

`OrisEventImportService.importEventFromOris` passes `null` as the acting user to `pullAndEnroll`, while the main spec scenario "The import appears in the event's synchronization history" says the history records the manager who performed the import. Either the scenario or the code is wrong. This is a behaviour question and belongs in its own change (it needs the acting user threaded from `OrisEventController` through `OrisEventImportPort`); recorded here so it is not lost.
