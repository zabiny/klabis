---
name: sync_module_tests_2026_09_06
description: Sync module and related tests after Instant.now() refactor (2026-09-06)
metadata:
  type: project
---

## Sync Module Tests Run (2026-09-06)

### Execution Details
- **Date/Time:** 2026-09-06 after Instant.now() refactor
- **Command:** Backend test-runner with filters for sync domain, application, infrastructure/jdbc, infrastructure/restapi, OrisEventSyncAdapter, calendar sync, and events sync
- **Exit Code:** 0 (SUCCESS)
- **Filter patterns:** `com.klabis.sync.*`, `com.klabis.oris.eventsync.*`, `com.klabis.calendar.application.*Sync*`, `com.klabis.events.*Sync*`

### Results
**ALL 257 SYNC-RELATED TESTS PASSED** ✓

- Total tests: 257
- Passed: 257 (100%)
- Failed: 0
- Skipped: 0
- Compilation: ✓ Success

### Test Coverage
Includes all sync module tests across:
- **Core sync domain:** SyncRecord conflict/failure/direction/field-attribution, SyncRecordConflictEvent
- **Sync application:** SynchronizationService integration/failure handling/mark-dirty/no-adapter, SyncScheduler, SyncHistoryRetentionJob, SyncProperties
- **Sync infrastructure:** SyncRecordJdbcRepository, SyncAttemptJdbcRepository, SynchronizationController, SyncProjectionCodec
- **ORIS event sync adapter:** OrisEventSyncAdapterIntegrationTest, OrisEventSyncAdapterTest, OrisEventSyncScenarioIntegrationTest
- **Calendar event sync:** CalendarEventSyncServiceTest, CalendarEventSyncIntegrationTest
- **Events sync:** EventsSyncListenerTest, OrisBulkSyncServiceTest

### Conclusion
No regressions detected in sync module after Instant.now() refactor. All call sites (main + test) successfully passing Instant parameter.
