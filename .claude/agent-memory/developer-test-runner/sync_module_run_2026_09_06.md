---
name: sync_module_test_run_2026_09_06
description: Sync module test run results after MutableClock implementation
metadata:
  type: project
---

## Sync Module Test Run (2026-09-06)

**Date**: 2026-09-06  
**Branch**: claude/klabis-oris-sync-engine-t9sy9q

### Scope
- `com.klabis.sync.*` (domain, application, infrastructure/jdbc, infrastructure/restapi)
- `com.klabis.oris.eventsync.*`
- Calendar sync tests: `CalendarEventSyncIntegrationTest`, `CalendarEventSyncServiceTest`
- Events sync tests: `EventsSyncListenerTest`

### Results
**Total**: 205 tests  
**Passed**: 204 (99.5%)  
**Failed**: 1 (0.5%)  
**Skipped**: 0  
**Duration**: 3m 44s

### Failure

**Test**: `com.klabis.sync.application.SyncSchedulerTest$FullPass.skips a terminally failed record (design.md D10 — skipped by the scheduler until reset)`

**Location**: `/backend/src/test/java/com/klabis/sync/application/SyncSchedulerTest.java:146`

**Error**:
```
org.opentest4j.AssertionFailedError: 
expected: FAILED
 but was: RETRYING
```

**Observation**: The test uses the new `MutableClock` fixture to control time via `@TestConfiguration` overriding the Clock bean. The assertion expects a record to reach FAILED state but it reached RETRYING instead — indicates time control may not be advancing as expected in the scheduler test, or the retry timing logic differs from the test's expectations.

### Tests Executed (23 test classes)
- **Sync domain**: FailureClassifierTest, SyncRecordConflictEventTest, SyncRecordDirectionResolutionTest, SyncRecordFailureHandlingTest, SyncRecordFieldAttributionTest
- **Sync application**: RetrySchedulerTest, SyncPropertiesTest, SyncSchedulerTest, SyncHistoryRetentionJobTest, 4x SynchronizationService*IntegrationTest
- **Sync infrastructure**: SyncAttemptJdbcRepositoryTest, SyncRecordJdbcRepositoryTest, SynchronizationControllerTest, SyncProjectionCodecTest
- **ORIS eventsync**: OrisEventProjectionMapperTest, OrisEventSyncAdapterIntegrationTest, OrisEventSyncAdapterTest, OrisEventSyncScenarioIntegrationTest
- **Calendar/Events sync**: CalendarEventSyncIntegrationTest, CalendarEventSyncServiceTest, EventsSyncListenerTest
