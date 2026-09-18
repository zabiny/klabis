## 1. Split the port

- [x] 1.1 Introduce a gateway interface in `events.application` carrying `readOrisFields` and `applyOrisSync`.
- [x] 1.2 Leave `importEventFromOris` and `syncEventFromOris` on the orchestration port.
- [x] 1.3 Wire the implementation so the gateway bean does not depend on `SynchronizationPort`.
- [x] 1.4 Expose the gateway through the module's named interface so `oris.eventsync` may reach it.

## 2. Remove the cycle

- [x] 2.1 Point `OrisEventSyncAdapter` at the gateway instead of `OrisEventImportPort`.
- [x] 2.2 Drop `@Lazy` from the adapter constructor and update the javadoc that explains why it was there.
- [x] 2.3 Start the application context and confirm it wires eagerly, with no cycle and no `@Lazy` needed.

## 3. Architecture and boundaries

- [x] 3.1 Run `ModuleStructureVerificationTest`, `LayerArchitectureTest`, `SecurityArchitectureTest` and `JMoleculesArchitectureTest` explicitly by name — a package-filtered run misses `com.klabis.JMoleculesArchitectureTest`.
- [x] 3.2 Confirm `OrisEventSyncAdapter` keeps its `@Application` classification and that the split does not reintroduce the `@PrimaryAdapter`/`@SecondaryAdapter` conflict.
- [x] 3.3 Decide whether `OrisEventFields` and `OrisEventDetailsMapper` move to `oris.eventsync`; record the decision either way.
  - Decision: both stay in `events.application`. `OrisEventFields` is part of the gateway port signature, and moving either type would force `events.application → oris.eventsync` — the reverse of the one-way direction (D2) this change restores. (13/13 arch tests pass; adapter keeps `@Application`.)

## 4. Verification

- [x] 4.1 Confirm ORIS import and ORIS sync still behave identically end to end.
  - Evidence: `OrisEventSyncScenarioIntegrationTest` (9/9) and `OrisEventSyncAdapterIntegrationTest` (3/3, full `@SpringBootTest` context) plus the import-path tests (`OrisEventImportServiceTest`, `OrisEventTypeAutoMappingTest`, `OrisEventControllerTest`) ran green on the refactored code; code review found no behavioral drift.
- [x] 4.2 Run the full backend test suite; all tests compile and pass.
  - 3418/3418 passed, 0 failed/skipped, exit 0 — identical to same-day clean-main baseline.
- [x] 4.3 Code review.
  - No high findings, no behavioral drift. Two medium doc-drift findings (stale ADR-005 notes, `backend-patterns` skill still teaching the `@Lazy` pattern) fixed in the follow-up commit.
- [x] 4.4 Commit.
