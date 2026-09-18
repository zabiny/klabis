---
name: sync_refactor_iteration_1_compile_errors
description: Compilation errors in sync tests after Instant parameter refactor
metadata:
  type: project
---

## Sync Module Refactor - Iteration 1 Compile Failures (2026-09-06)

### Summary
Domain method signatures in sync module changed to accept `Instant` parameters. Test call sites do not pass these parameters, causing **32 compilation errors** across 15 test files. **Tests do not compile; cannot execute.**

### Compilation Errors by Test File

1. **RetrySchedulerTest.java:22** (1 error)
   - `SyncAttempt.record()` signature changed to require `Instant` as 2nd parameter
   - Current call: `SyncAttempt.record(RECORD_ID, SyncTriggerKind.SCHEDULED, null, outcome, null, null, null, null)`
   - Expected: `record(SyncRecordId, Instant, SyncTriggerKind, SyncDirection, SyncOutcome, SyncHash, SyncHash, String, String)`

2. **SyncSchedulerTest.java** (3 errors)
   - Lines 145, 172, 188: `SyncRecord.markDirty()` now requires `Instant` parameter
   - Current: `marked.markDirty()` 
   - Expected: `markDirty(Instant)`

3. **SyncHistoryRetentionJobTest.java:57** (1 error)
   - `SyncRecord.recordSuccess()` now requires 4 parameters (added `Instant`)
   - Current: `record.recordSuccess(SyncDirection.INWARD, agreed, agreed)`
   - Expected: `recordSuccess(SyncDirection, SyncSnapshot, SyncSnapshot, Instant)`

4. **SyncRecordFieldAttributionTest.java** (6 errors)
   - Lines 38, 42, 53, 57, 68, 72, 83 etc.: Mixed failures
   - `recordSuccess()` missing `Instant` parameter (lines 38, 53, 68, 83)
   - `recordConflict()` signature changed; now requires `(SyncSnapshot, SyncSnapshot, SyncDirection, Instant)` instead of `(SyncSnapshot, SyncSnapshot, SyncDirection)`

5. **SyncRecordConflictEventTest.java** (1 error)
   - `recordConflict()` missing `Instant` parameter

6. **SyncRecordDirectionResolutionTest.java** (3 errors)
   - `recordSuccess()` and `recordConflict()` missing `Instant` parameters

7. **SyncRecordFailureHandlingTest.java** (5 errors)
   - `recordTerminalFailure()` now requires 3 parameters (added `Instant` as 3rd)
   - `recordSuccess()` missing `Instant`
   - `markDirty()` missing `Instant`

8. **SyncAttemptJdbcRepositoryTest.java** (1 error)
   - `SyncAttempt.record()` missing `Instant` parameter

9. **SyncRecordJdbcRepositoryTest.java** (8 errors)
   - Multiple methods: `recordSuccess()`, `recordConflict()`, `recordTerminalFailure()`, `markDirty()`
   - Lines: 296, 313, 314, 332, 333, 346, 347, 348, 366, 367

10. **SynchronizationControllerTest.java** (2 errors)
    - `recordSuccess()` and `recordConflict()` missing `Instant` parameters

### Domain Method Signatures Changed

Based on error messages, these signatures now require `Instant`:

- `SyncAttempt.record(SyncRecordId, Instant, SyncTriggerKind, SyncDirection, SyncOutcome, SyncHash, SyncHash, String, String)`
- `SyncRecord.markDirty(Instant)`
- `SyncRecord.recordSuccess(SyncDirection, SyncSnapshot, SyncSnapshot, Instant)`
- `SyncRecord.recordConflict(SyncSnapshot, SyncSnapshot, SyncDirection, Instant)`
- `SyncRecord.recordTerminalFailure(int, String, Instant)`

### Application-Layer Workaround
Per the task description, application-layer call sites currently pass `Instant.now()` as a temporary solution.

### Next Steps Required
Test call sites must be updated to pass appropriate `Instant` values (likely `Instant.now()` for most cases, or use fixtures/constants for deterministic tests).
