## 1. Remove the unused operation

- [ ] 1.1 Add a test fixture that creates a paired `SyncRecord` without the port (via `SyncRecordRepository` and the schedule), and switch the three integration tests to it (`SynchronizationServiceIntegrationTest`, `SynchronizationServiceNoAdapterIntegrationTest`, `OrisEventRegistrationSyncScenarioIntegrationTest`). Check that `SynchronizationServiceNoAdapterIntegrationTest` still covers the "no adapter registered" failure, now through `pullAndEnroll`.
- [ ] 1.2 Delete `enroll` from `SynchronizationPort` and `SynchronizationService`; fix the Javadoc that links to it
- [ ] 1.3 Run the sync and events backend tests; `grep -rn "\.enroll(" backend/src` finds only `SyncRecord.enroll` (the domain factory) and `pullAndEnroll`

## 2. Finish

- [ ] 2.1 Full backend test suite, code review, commit
