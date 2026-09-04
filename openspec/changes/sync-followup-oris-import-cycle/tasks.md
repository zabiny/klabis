## 1. Split the port

- [ ] 1.1 Introduce a gateway interface in `events.application` carrying `readOrisFields` and `applyOrisSync`.
- [ ] 1.2 Leave `importEventFromOris` and `syncEventFromOris` on the orchestration port.
- [ ] 1.3 Wire the implementation so the gateway bean does not depend on `SynchronizationPort`.
- [ ] 1.4 Expose the gateway through the module's named interface so `oris.eventsync` may reach it.

## 2. Remove the cycle

- [ ] 2.1 Point `OrisEventSyncAdapter` at the gateway instead of `OrisEventImportPort`.
- [ ] 2.2 Drop `@Lazy` from the adapter constructor and update the javadoc that explains why it was there.
- [ ] 2.3 Start the application context and confirm it wires eagerly, with no cycle and no `@Lazy` needed.

## 3. Architecture and boundaries

- [ ] 3.1 Run `ModuleStructureVerificationTest`, `LayerArchitectureTest`, `SecurityArchitectureTest` and `JMoleculesArchitectureTest` explicitly by name — a package-filtered run misses `com.klabis.JMoleculesArchitectureTest`.
- [ ] 3.2 Confirm `OrisEventSyncAdapter` keeps its `@Application` classification and that the split does not reintroduce the `@PrimaryAdapter`/`@SecondaryAdapter` conflict.
- [ ] 3.3 Decide whether `OrisEventFields` and `OrisEventDetailsMapper` move to `oris.eventsync`; record the decision either way.

## 4. Verification

- [ ] 4.1 Confirm ORIS import and ORIS sync still behave identically end to end.
- [ ] 4.2 Run the full backend test suite; all tests compile and pass.
- [ ] 4.3 Code review.
- [ ] 4.4 Commit.
